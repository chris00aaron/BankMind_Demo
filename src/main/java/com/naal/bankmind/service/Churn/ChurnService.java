package com.naal.bankmind.service.Churn;

import com.naal.bankmind.dto.Churn.CampaignLogDTO;
import com.naal.bankmind.dto.Churn.ChurnBatchResultItemDTO;
import com.naal.bankmind.dto.Churn.ChurnRequestDTO;
import com.naal.bankmind.dto.Churn.RiskClientDTO;
import com.naal.bankmind.dto.Churn.RiskIntelligenceDTO;
import com.naal.bankmind.entity.Churn.ChurnSampleBatch;
import com.naal.bankmind.entity.Churn.ChurnSampleEntry;
import com.naal.bankmind.repository.Churn.ChurnSampleBatchRepository;
import com.naal.bankmind.repository.Churn.ChurnSampleEntryRepository;
import com.naal.bankmind.dto.Churn.ChurnResponseDTO;
import com.naal.bankmind.dto.Churn.CustomerDashboardDTO;
import com.naal.bankmind.dto.Churn.SegmentDTO;
import com.naal.bankmind.dto.Churn.TrainResultDTO;
import com.naal.bankmind.dto.Churn.PerformanceStatusDTO;
import com.naal.bankmind.dto.Churn.TrainingHistoryPointDTO;
import com.naal.bankmind.dto.Churn.PredictionBucketDTO;
import java.util.Collections;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.ChurnPredictions;
import com.naal.bankmind.entity.ChurnTrainingHistory;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.CustomerRepository;
import com.naal.bankmind.repository.Churn.ChurnPredictionsRepository;
import com.naal.bankmind.repository.Churn.RetentionStrategyDefRepository;
import com.naal.bankmind.repository.Churn.RetentionSegmentDefRepository;
import com.naal.bankmind.repository.Churn.CampaignLogRepository;
import com.naal.bankmind.repository.Churn.CampaignTargetRepository;
import com.naal.bankmind.repository.Churn.ChurnTrainingHistoryRepository;
import com.naal.bankmind.entity.RetentionStrategyDef;
import com.naal.bankmind.entity.RetentionSegmentDef;
import com.naal.bankmind.entity.CampaignLog;
import com.naal.bankmind.entity.CampaignTarget;
import com.naal.bankmind.entity.CampaignTargetKey;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Customer Churn Predictions.
 * 
 * Responsibilities:
 * - predictRealCustomer: Analyzes existing customer and saves to DB
 * - simulateScenario: What-If simulation WITHOUT saving to DB
 * - getHistory: Gets historical predictions for a customer
 * - getAllCustomersForDashboard: Gets all customers for dashboard display
 */
@Service
@Transactional
public class ChurnService {

    // Cache for the latest training result (used by getMLOpsMetrics)
    private static volatile TrainResultDTO lastTrainResult = null;
    private static volatile String lastTrainResultDate = null; // fecha real del entrenamiento

    private final CustomerRepository customerRepository;
    private final AccountDetailsRepository accountDetailsRepository;
    private final ChurnPredictionsRepository churnPredictionsRepository;
    private final RetentionStrategyDefRepository retentionStrategyDefRepository;
    private final RetentionSegmentDefRepository retentionSegmentDefRepository;
    private final CampaignLogRepository campaignLogRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    private final ChurnTrainingHistoryRepository churnTrainingHistoryRepository;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    private ChurnSampleBatchRepository churnSampleBatchRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ChurnSampleEntryRepository churnSampleEntryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${churn.api.base-url:http://localhost:8001}")
    private String churnApiBaseUrl;

    /**
     * Máximo de clientes en la muestra estratificada para la Matriz de Prioridad de
     * Retención.
     */
    @Value("${churn.matrix.sample-size:500}")
    private int matrixSampleSize;

    public ChurnService(
            CustomerRepository customerRepository,
            AccountDetailsRepository accountDetailsRepository,
            ChurnPredictionsRepository churnPredictionsRepository,
            RetentionStrategyDefRepository retentionStrategyDefRepository,
            RetentionSegmentDefRepository retentionSegmentDefRepository,
            CampaignLogRepository campaignLogRepository,
            CampaignTargetRepository campaignTargetRepository,
            ChurnTrainingHistoryRepository churnTrainingHistoryRepository) {
        this.customerRepository = customerRepository;
        this.accountDetailsRepository = accountDetailsRepository;
        this.churnPredictionsRepository = churnPredictionsRepository;
        this.retentionStrategyDefRepository = retentionStrategyDefRepository;
        this.retentionSegmentDefRepository = retentionSegmentDefRepository;
        this.campaignLogRepository = campaignLogRepository;
        this.campaignTargetRepository = campaignTargetRepository;
        this.churnTrainingHistoryRepository = churnTrainingHistoryRepository;
        // M7: RestTemplate with timeouts to prevent indefinite blocking
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds to connect
        factory.setReadTimeout(600000); // 10 minutes to read (training can be slow)
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Gets customers for the dashboard display with server-side pagination.
     * Uses batch queries to eliminate N+1 performance issues.
     * Also calculates KPIs globally (not just for the current page).
     *
     * @param page      Page number (0-indexed)
     * @param size      Page size (default 50)
     * @param search    Optional search term (name or ID)
     * @param country   Optional country filter (e.g. "Spain")
     * @param riskLevel Optional risk level filter: "alto" (>70), "medio" (45-70),
     *                  "bajo" (<45)
     * @return CustomerPageDTO with paginated content and global KPIs
     */
    public com.naal.bankmind.dto.Churn.CustomerPageDTO getCustomersPaginated(int page, int size, String search,
            String country, String riskLevel) {
        return getCustomersPaginated(page, size, search, country, riskLevel, null);
    }

    public com.naal.bankmind.dto.Churn.CustomerPageDTO getCustomersPaginated(int page, int size, String search,
            String country, String riskLevel, String segment) {
        System.out.println("Service: Fetching customers page=" + page + " size=" + size + " search='" + search
                + "' country='" + country + "' riskLevel='" + riskLevel + "'");

        boolean hasRiskFilter = riskLevel != null && !riskLevel.trim().isEmpty();
        boolean hasCountryFilter = country != null && !country.trim().isEmpty();
        boolean hasSegmentFilter = segment != null && !segment.trim().isEmpty();
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasFilters = hasRiskFilter || hasCountryFilter || hasSegmentFilter;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Customer> customerPage;

        if (hasFilters) {
            // ── Pre-filter: resolve eligible customer IDs at DB level ────────
            java.util.Set<Long> eligibleIds = null;

            if (hasRiskFilter) {
                List<Long> riskIds = churnPredictionsRepository
                        .findCustomerIdsByLatestRiskLevel(riskLevel.trim());
                eligibleIds = new java.util.HashSet<>(riskIds);
            }

            if (hasCountryFilter) {
                List<Long> countryIds = customerRepository.findIdsByCountry(country.trim());
                java.util.Set<Long> countrySet = new java.util.HashSet<>(countryIds);
                eligibleIds = (eligibleIds == null) ? countrySet : intersect(eligibleIds, countrySet);
            }

            if (hasSegmentFilter) {
                java.math.BigDecimal balanceMin = null;
                java.math.BigDecimal balanceMax = null;
                switch (segment.trim().toLowerCase()) {
                    case "corporate":
                        balanceMin = java.math.BigDecimal.valueOf(100000);
                        break;
                    case "sme":
                        balanceMin = java.math.BigDecimal.valueOf(50000);
                        balanceMax = java.math.BigDecimal.valueOf(100000);
                        break;
                    case "personal":
                        balanceMax = java.math.BigDecimal.valueOf(50000);
                        break;
                    default:
                        break;
                }
                List<Long> segmentIds = accountDetailsRepository.findCustomerIdsByBalanceRange(balanceMin, balanceMax);
                java.util.Set<Long> segmentSet = new java.util.HashSet<>(segmentIds);
                eligibleIds = (eligibleIds == null) ? segmentSet : intersect(eligibleIds, segmentSet);
            }

            if (eligibleIds == null || eligibleIds.isEmpty()) {
                com.naal.bankmind.dto.Churn.DashboardKpisDTO emptyKpis = calculateDashboardKpis(search);
                return com.naal.bankmind.dto.Churn.CustomerPageDTO.builder()
                        .content(new ArrayList<>())
                        .totalElements(0L)
                        .totalPages(0)
                        .page(page)
                        .size(size)
                        .kpis(emptyKpis)
                        .build();
            }

            // ── Paginate within eligible IDs, sorted by risk DESC ────────────
            if (hasSearch) {
                customerPage = customerRepository.findByIdCustomerInAndSearchOrderByRiskDesc(
                        eligibleIds, search.trim(), pageable);
            } else {
                customerPage = customerRepository.findByIdCustomerInOrderByRiskDesc(eligibleIds, pageable);
            }

        } else if (hasSearch) {
            customerPage = customerRepository.searchByNameOrIdOrderByRiskDesc(search.trim(), pageable);
        } else {
            // Default: all customers, highest-risk first
            customerPage = customerRepository.findAllOrderByRiskDesc(pageable);
        }

        List<Customer> customers = customerPage.getContent();
        System.out.println("Service: Page has " + customers.size() + " customers (total: "
                + customerPage.getTotalElements() + ")");

        // ── Batch load accounts and predictions for this page ────────────────
        List<Long> customerIds = customers.stream()
                .map(Customer::getIdCustomer)
                .collect(Collectors.toList());

        Map<Long, AccountDetails> accountMap = new java.util.HashMap<>();
        Map<Long, ChurnPredictions> predictionMap = new java.util.HashMap<>();

        if (!customerIds.isEmpty()) {
            List<AccountDetails> accounts = accountDetailsRepository.findByCustomerIds(customerIds);
            for (AccountDetails ad : accounts) {
                accountMap.putIfAbsent(ad.getCustomer().getIdCustomer(), ad);
            }
            List<ChurnPredictions> predictions = churnPredictionsRepository.findLatestByCustomerIds(customerIds);
            for (ChurnPredictions cp : predictions) {
                predictionMap.putIfAbsent(cp.getCustomer().getIdCustomer(), cp);
            }
        }

        // ── Build DTOs (no in-memory filtering needed — already done at DB level) ─
        List<CustomerDashboardDTO> content = new ArrayList<>();
        for (Customer customer : customers) {
            try {
                CustomerDashboardDTO dto = buildCustomerDTO(
                        customer,
                        accountMap.get(customer.getIdCustomer()),
                        predictionMap.get(customer.getIdCustomer()));
                content.add(dto);
            } catch (Exception e) {
                System.err.println("Error processing customer ID " + customer.getIdCustomer() + ": " + e.getMessage());
            }
        }

        // ── Global KPIs (always over the full dataset) ───────────────────────
        com.naal.bankmind.dto.Churn.DashboardKpisDTO kpis = calculateDashboardKpis(search);

        return com.naal.bankmind.dto.Churn.CustomerPageDTO.builder()
                .content(content)
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .page(page)
                .size(size)
                .kpis(kpis)
                .build();
    }

    /**
     * Fetches a single customer by ID for the detail page.
     * Uses the same buildCustomerDTO logic as the paginated list.
     *
     * @param idCustomer Customer ID
     * @return CustomerDashboardDTO with latest prediction, or empty if not found
     */
    public Optional<CustomerDashboardDTO> getCustomerById(Long idCustomer) {
        return customerRepository.findById(idCustomer).map(customer -> {
            AccountDetails account = accountDetailsRepository
                    .findFirstByCustomer_IdCustomer(idCustomer).orElse(null);
            List<ChurnPredictions> history = churnPredictionsRepository
                    .findByCustomer_IdCustomerOrderByPredictionDateDesc(idCustomer);
            ChurnPredictions latest = history.isEmpty() ? null : history.get(0);
            return buildCustomerDTO(customer, account, latest);
        });
    }

    /**
     * Builds a single CustomerDashboardDTO from pre-loaded data.
     * No additional database queries are made.
     */
    private CustomerDashboardDTO buildCustomerDTO(Customer customer, AccountDetails account,
            ChurnPredictions prediction) {
        Integer creditScore = 0;
        BigDecimal balance = BigDecimal.ZERO;
        Integer products = 0;

        if (account != null) {
            creditScore = account.getCreditScore() != null ? account.getCreditScore() : 0;
            balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            products = account.getNumOfProducts() != null ? account.getNumOfProducts() : 1;
        }

        // Risk score: from latest prediction, or null if not yet predicted.
        // null = frontend shows "—" (sin predicción). Never defaults to 50.
        Integer riskScore = null;
        if (prediction != null && prediction.getChurnProbability() != null) {
            riskScore = prediction.getChurnProbability().multiply(BigDecimal.valueOf(100)).intValue();
        }

        // Country name
        String country = "Desconocido";
        if (customer.getCountry() != null && customer.getCountry().getCountryDescription() != null) {
            country = customer.getCountry().getCountryDescription();
        }

        // Tenure and since from registration date
        Integer tenure = 0;
        String since = "N/A";
        if (customer.getIdRegistrationDate() != null) {
            java.time.LocalDateTime regDate = customer.getIdRegistrationDate();
            tenure = (int) java.time.temporal.ChronoUnit.YEARS.between(regDate, java.time.LocalDateTime.now());
            since = String.valueOf(regDate.getYear());
        }

        return CustomerDashboardDTO.builder()
                .id(customer.getIdCustomer())
                .score(creditScore)
                .age(customer.getAge() != null ? customer.getAge() : 0)
                .balance(balance)
                .country(country)
                .name((customer.getFirstName() != null ? customer.getFirstName() : "") + " "
                        + (customer.getSurname() != null ? customer.getSurname() : ""))
                .risk(riskScore)
                .tenure(tenure)
                .since(since)
                .products(products)
                .email(customer.getEmail()) // Real email from DB — used by frontend contact button
                .build();
    }

    /**
     * Calculates global KPIs for the dashboard.
     * Uses batch queries to compute aggregated stats + Top 200 scatter data.
     */
    /** Returns the intersection of two sets (modifies neither). */
    private java.util.Set<Long> intersect(java.util.Set<Long> a, java.util.Set<Long> b) {
        java.util.Set<Long> result = new java.util.HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private com.naal.bankmind.dto.Churn.DashboardKpisDTO calculateDashboardKpis(String search) {
        // ── Total real de clientes en BD (sin cap) ──────────────────────────
        long totalCustomers = customerRepository.count();

        // ── Todos los clientes con predicción disponible ─────────────────────
        // Partimos de churn_predictions para no cargar clientes sin predicción.
        // En búsqueda se filtra post-carga por nombre/ID.
        List<ChurnPredictions> allLatestPredictions = churnPredictionsRepository.findLatestForAllCustomers();

        List<Customer> allCustomers = allLatestPredictions.stream()
                .map(ChurnPredictions::getCustomer)
                .collect(Collectors.toList());

        // Filtrar por búsqueda si aplica
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            allCustomers = allCustomers.stream()
                    .filter(c -> {
                        String name = ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                                + (c.getSurname() != null ? c.getSurname() : "")).toLowerCase();
                        return name.contains(q) || String.valueOf(c.getIdCustomer()).contains(q);
                    })
                    .collect(Collectors.toList());
            totalCustomers = allCustomers.size();
        }

        List<Long> allIds = allCustomers.stream().map(Customer::getIdCustomer).collect(Collectors.toList());

        Map<Long, AccountDetails> allAccounts = new java.util.HashMap<>();
        Map<Long, ChurnPredictions> allPredictions = new java.util.HashMap<>();

        if (!allIds.isEmpty()) {
            for (AccountDetails ad : accountDetailsRepository.findByCustomerIds(allIds)) {
                allAccounts.putIfAbsent(ad.getCustomer().getIdCustomer(), ad);
            }
            for (ChurnPredictions cp : allLatestPredictions) {
                allPredictions.putIfAbsent(cp.getCustomer().getIdCustomer(), cp);
            }
        }

        long customersAtRisk = 0;
        BigDecimal capitalAtRisk = BigDecimal.ZERO;
        long highRiskCount = 0;
        long mediumRiskCount = 0;
        long lowRiskCount = 0;

        // Collect risk+balance pairs for scatter chart sorting
        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> allPoints = new ArrayList<>();

        for (Customer c : allCustomers) {
            AccountDetails acc = allAccounts.get(c.getIdCustomer());
            ChurnPredictions pred = allPredictions.get(c.getIdCustomer());

            boolean hasAccount = acc != null && acc.getBalance() != null
                    && acc.getBalance().compareTo(BigDecimal.ZERO) > 0;
            boolean hasPrediction = pred != null && pred.getChurnProbability() != null;

            BigDecimal balance = hasAccount ? acc.getBalance() : BigDecimal.ZERO;
            int risk = 50; // default cuando no hay predicción
            if (hasPrediction) {
                risk = pred.getChurnProbability().multiply(BigDecimal.valueOf(100)).intValue();
            }

            // Risk distribution counting — solo clientes con predicción real
            if (hasPrediction) {
                if (risk > 70) {
                    highRiskCount++;
                } else if (risk >= 45) {
                    mediumRiskCount++;
                } else {
                    lowRiskCount++;
                }

                if (risk >= 45) {
                    customersAtRisk++;
                    capitalAtRisk = capitalAtRisk
                            .add(balance.multiply(BigDecimal.valueOf(risk)).divide(BigDecimal.valueOf(100),
                                    java.math.RoundingMode.HALF_UP));
                }
            }

            // Solo agregar al scatter si tiene predicción real — evita colapsar todos en
            // (50, balance)
            if (!hasPrediction)
                continue;

            String name = ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                    + (c.getSurname() != null ? c.getSurname() : "")).trim();

            String countryName = "Desconocido";
            if (c.getCountry() != null && c.getCountry().getCountryDescription() != null) {
                countryName = c.getCountry().getCountryDescription();
            }

            allPoints.add(com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO.builder()
                    .x(risk)
                    .y(balance.doubleValue())
                    .z(100)
                    .name(name)
                    .id(c.getIdCustomer())
                    .country(countryName)
                    .build());
        }

        // retentionRate = % of PREDICTED customers that are low-risk (<45%).
        // Using only predicted customers as denominator avoids inflating the rate
        // by counting unpredicted customers as "retained" by default.
        long predictedTotal = highRiskCount + mediumRiskCount + lowRiskCount;
        double retentionRate = predictedTotal > 0
                ? ((double) lowRiskCount / predictedTotal) * 100.0
                : 0.0;

        // ── Stratified scatter sample: up to 150 per quadrant (max 600 total) ──────
        // Thresholds mirror the frontend constants (RISK_THRESHOLD=45,
        // BALANCE_THRESHOLD=100000)
        final int RISK_THRESHOLD = 45;
        final long BALANCE_THRESHOLD = 100_000L;
        final int MAX_PER_QUADRANT = 150;

        // Partition into 4 quadrants
        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> danger = new ArrayList<>();
        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> watch = new ArrayList<>();
        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> vip = new ArrayList<>();
        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> safe = new ArrayList<>();

        for (com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO p : allPoints) {
            boolean highRisk = p.getX() > RISK_THRESHOLD;
            boolean highBalance = p.getY() > BALANCE_THRESHOLD;
            if (highRisk && highBalance)
                danger.add(p);
            else if (highRisk && !highBalance)
                watch.add(p);
            else if (!highRisk && highBalance)
                vip.add(p);
            else
                safe.add(p);
        }

        // Shuffle each bucket to avoid index-order bias, then cap at MAX_PER_QUADRANT
        java.util.Collections.shuffle(danger, new java.util.Random(42));
        java.util.Collections.shuffle(watch, new java.util.Random(42));
        java.util.Collections.shuffle(vip, new java.util.Random(42));
        java.util.Collections.shuffle(safe, new java.util.Random(42));

        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> scatterData = new ArrayList<>();
        scatterData.addAll(danger.stream().limit(MAX_PER_QUADRANT).collect(Collectors.toList()));
        scatterData.addAll(watch.stream().limit(MAX_PER_QUADRANT).collect(Collectors.toList()));
        scatterData.addAll(vip.stream().limit(MAX_PER_QUADRANT).collect(Collectors.toList()));
        scatterData.addAll(safe.stream().limit(MAX_PER_QUADRANT).collect(Collectors.toList()));

        System.out.println(String.format(
                "[ScatterSample] danger=%d watch=%d vip=%d safe=%d → total=%d",
                Math.min(danger.size(), MAX_PER_QUADRANT),
                Math.min(watch.size(), MAX_PER_QUADRANT),
                Math.min(vip.size(), MAX_PER_QUADRANT),
                Math.min(safe.size(), MAX_PER_QUADRANT),
                scatterData.size()));

        return com.naal.bankmind.dto.Churn.DashboardKpisDTO.builder()
                .totalCustomers(totalCustomers)
                .customersAtRisk(customersAtRisk)
                .capitalAtRisk(capitalAtRisk)
                .retentionRate(Math.round(retentionRate * 10.0) / 10.0)
                .scatterData(scatterData)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .build();
    }

    /**
     * Analyzes an existing customer in the DB and saves the prediction.
     * 
     * @param idCustomer Customer ID to analyze
     * @return The full prediction response DTO (including risk factors)
     * @throws RuntimeException if customer doesn't exist or has no financial data
     */
    public ChurnResponseDTO predictRealCustomer(Long idCustomer) {
        // 1. Find customer
        Customer customer = customerRepository.findById(idCustomer)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + idCustomer));

        // 2. Find customer financial data
        AccountDetails accountDetails = accountDetailsRepository.findFirstByCustomer_IdCustomer(idCustomer)
                .orElseThrow(
                        () -> new RuntimeException("Financial data not found for customer: " + idCustomer));

        // 3. Map to DTO for Python API
        ChurnRequestDTO requestDTO = mapToChurnRequest(customer, accountDetails);

        // 4. Call Python API
        ChurnResponseDTO responseDTO = callPythonApi(requestDTO);

        // 5. Save prediction to DB (Entity)
        ChurnPredictions prediction = new ChurnPredictions();
        prediction.setCustomer(customer);
        prediction.setPredictionDate(LocalDateTime.now());
        prediction.setChurnProbability(BigDecimal.valueOf(responseDTO.getChurnProbability()));
        prediction.setIsChurn(responseDTO.getIsChurn());
        prediction.setRiskLevel(responseDTO.getRiskLevel());
        prediction.setModelVersion(responseDTO.getModelVersion() != null ? responseDTO.getModelVersion() : "v1");

        // Set new fields
        BigDecimal balance = accountDetails.getBalance() != null ? accountDetails.getBalance() : BigDecimal.ZERO;
        prediction.setCustomerValue(balance);

        // Default confidence to 0.95 if not provided by API
        Double confidence = responseDTO.getPredictionConfidence() != null ? responseDTO.getPredictionConfidence()
                : 0.95;
        prediction.setPredictionConfidence(BigDecimal.valueOf(confidence));

        churnPredictionsRepository.save(prediction);

        // Return the DTO because it contains the Risk Factors (XAI)
        return responseDTO;
    }

    /**
     * Simulates a What-If scenario WITHOUT saving to DB.
     * 
     * @param simulatedData Simulated scenario data
     * @return Prediction response (volatile, not saved)
     */
    public ChurnResponseDTO simulateScenario(ChurnRequestDTO simulatedData) {
        return callPythonApi(simulatedData);
    }

    /**
     * Gets the prediction history for a customer.
     * 
     * @param idCustomer Customer ID
     * @return List of predictions ordered by date descending
     */
    public List<ChurnPredictions> getHistory(Long idCustomer) {
        return churnPredictionsRepository.findByCustomer_IdCustomerOrderByPredictionDateDesc(idCustomer);
    }

    /**
     * Generates a "Next Best Action" recommendation based on realistic banking
     * logic.
     * 
     * Business Rationale:
     * 1. VIP (>100k): Status and yield (Premium Upgrade).
     * 2. Mono-product: Cross-selling to increase stickiness (Pre-approved Card).
     * 3. Standard: Psychological relief from fees (Fee Waiver).
     * 
     * @param idCustomer Customer ID
     * @return The recommended strategy definition
     */
    public com.naal.bankmind.dto.Churn.RecommendationDTO getRecommendation(Long idCustomer) {
        // 1. Fetch customer context
        Customer customer = customerRepository.findById(idCustomer)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        AccountDetails account = accountDetailsRepository.findFirstByCustomer_IdCustomer(idCustomer)
                .orElseThrow(() -> new RuntimeException("Customer data not found"));

        // 2. Fetch latest risk prediction
        List<ChurnPredictions> history = churnPredictionsRepository
                .findByCustomer_IdCustomerOrderByPredictionDateDesc(idCustomer);
        double currentRisk = history.isEmpty() ? 0.5 : history.get(0).getChurnProbability().doubleValue();

        // 3. Rule engine: pick strategy
        long strategyId = 1L;
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        int products = account.getNumOfProducts() != null ? account.getNumOfProducts() : 0;

        if (balance.compareTo(new BigDecimal("100000")) > 0) {
            strategyId = 3L;
        } else if (products == 1 && currentRisk > 0.3) {
            strategyId = 2L;
        }

        // 4. Fetch static strategy from DB
        RetentionStrategyDef strategy = retentionStrategyDefRepository.findById(strategyId)
                .orElseThrow(() -> new RuntimeException("Strategy definition not found in DB."));

        // 5. Build DTO (copy of entity — avoids Hibernate dirty-check)
        com.naal.bankmind.dto.Churn.RecommendationDTO dto = new com.naal.bankmind.dto.Churn.RecommendationDTO();
        dto.setId(strategy.getIdStrategy());
        dto.setName(strategy.getName());
        dto.setDescription(strategy.getDescription());
        dto.setCostPerClient(strategy.getCostPerClient());
        dto.setIsActive(strategy.getIsActive());
        dto.setImpactFactor(strategy.getImpactFactor()); // static fallback

        // 6. Compute dynamic impactFactor via ML simulation
        try {
            ChurnRequestDTO baseRequest = mapToChurnRequest(customer, account);

            // Clone and apply strategy-specific intervention
            ChurnRequestDTO simRequest = ChurnRequestDTO.builder()
                    .creditScore(baseRequest.getCreditScore())
                    .geography(baseRequest.getGeography())
                    .gender(baseRequest.getGender())
                    .age(baseRequest.getAge())
                    .tenure(baseRequest.getTenure())
                    .balance(baseRequest.getBalance())
                    .numOfProducts(baseRequest.getNumOfProducts())
                    .hasCrCard(baseRequest.getHasCrCard())
                    .isActiveMember(baseRequest.getIsActiveMember())
                    .estimatedSalary(baseRequest.getEstimatedSalary())
                    .exited(baseRequest.getExited())
                    .build();

            // Apply intervention based on strategy
            switch ((int) (long) strategyId) {
                case 1: // Fee Waiver → financial relief re-activates customer
                    simRequest.setIsActiveMember(1);
                    break;
                case 2: // Pre-approved Card → adds product and card
                    simRequest.setHasCrCard(1);
                    simRequest.setNumOfProducts(Math.min(products + 1, 4));
                    break;
                case 3: // VIP Premium → dedicated manager re-activates + adds product
                    simRequest.setIsActiveMember(1);
                    simRequest.setNumOfProducts(Math.min(products + 1, 4));
                    break;
            }

            ChurnResponseDTO simResponse = callPythonApi(simRequest);
            if (simResponse != null && currentRisk > 0) {
                double postRisk = simResponse.getChurnProbability();
                double reduction = (currentRisk - postRisk) / currentRisk;
                // Clamp to [0, 1] — never show negative reduction or > 100%
                reduction = Math.max(0.0, Math.min(1.0, reduction));
                dto.setImpactFactor(BigDecimal.valueOf(reduction).setScale(4, java.math.RoundingMode.HALF_UP));
            }
        } catch (Exception e) {
            // Fallback silencioso: mantiene el impactFactor estático de la BD
            System.out.println("[ChurnService] Dynamic impactFactor failed, using static: " + e.getMessage());
        }

        return dto;
    }

    /**
     * Logs an interaction with the customer (e.g., Offer Sent).
     * 
     * @param idCustomer Customer ID
     * @param actionType The type of action (EMAIL, CALL, etc.)
     */
    public void logInteraction(Long idCustomer, String actionType) {
        // Find or create the "Interacciones Individuales" campaign without forcing a
        // manual ID
        // (campaign_log.id_campaign is GENERATED ALWAYS AS IDENTITY in PostgreSQL)
        CampaignLog campaign = campaignLogRepository.findByName("Interacciones Individuales")
                .orElseGet(() -> {
                    CampaignLog newCamp = new CampaignLog();
                    newCamp.setName("Interacciones Individuales");
                    newCamp.setStatus("ACTIVE");
                    newCamp.setStartDate(LocalDateTime.now());
                    return campaignLogRepository.save(newCamp);
                });

        // Find existing target entry or create a new one
        CampaignTargetKey key = new CampaignTargetKey();
        key.setIdCampaign(campaign.getIdCampaign());
        key.setIdCustomer(idCustomer);

        CampaignTarget target = campaignTargetRepository.findById(key).orElse(new CampaignTarget());
        target.setId(key);
        target.setStatus("CONTACTED");
        target.setContactDate(LocalDateTime.now());

        campaignTargetRepository.save(target);
        System.out.println("Interaction logged for customer " + idCustomer + ": " + actionType);
    }

    /**
     * Triggers the auto-training process in the Python API.
     * Returns structured metrics from the training run.
     * 
     * @return TrainResultDTO with metrics, run_id, and status
     */
    @SuppressWarnings("unchecked")
    public TrainResultDTO trainModel() {
        try {
            String url = churnApiBaseUrl + "/churn/train";
            System.out.println("---> Calling Python Auto-Training API: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            // Python returns a flat JSON with snake_case keys
            Map<String, Object> rawResponse = restTemplate.postForObject(url, entity, Map.class);

            if (rawResponse == null) {
                return TrainResultDTO.builder()
                        .status("error")
                        .error("La API de Python no devolvió respuesta.")
                        .build();
            }

            System.out.println("---> Python Training Response: " + rawResponse);

            // Map the response to our DTO
            TrainResultDTO.TrainResultDTOBuilder builder = TrainResultDTO.builder()
                    .status((String) rawResponse.getOrDefault("status", "unknown"))
                    .message((String) rawResponse.get("message"))
                    .runId((String) rawResponse.get("run_id"))
                    .versionTag((String) rawResponse.get("version_tag"))
                    .error((String) rawResponse.get("error"))
                    .promoted(rawResponse.get("promoted") instanceof Boolean
                            ? (Boolean) rawResponse.get("promoted")
                            : null)
                    .promotionReason((String) rawResponse.get("promotion_reason"))
                    .inProduction(rawResponse.get("in_production") instanceof Boolean
                            ? (Boolean) rawResponse.get("in_production")
                            : null);

            // Parse train/test samples
            if (rawResponse.get("train_samples") != null) {
                builder.trainSamples(((Number) rawResponse.get("train_samples")).intValue());
            }
            if (rawResponse.get("test_samples") != null) {
                builder.testSamples(((Number) rawResponse.get("test_samples")).intValue());
            }

            // Parse nested metrics object (challenger)
            Object metricsObj = rawResponse.get("metrics");
            if (metricsObj instanceof Map) {
                Map<String, Object> metricsMap = (Map<String, Object>) metricsObj;
                TrainResultDTO.TrainMetrics metrics = new TrainResultDTO.TrainMetrics();
                metrics.setAccuracy(toDouble(metricsMap.get("accuracy")));
                metrics.setF1Score(toDouble(metricsMap.get("f1_score")));
                metrics.setPrecision(toDouble(metricsMap.get("precision")));
                metrics.setRecall(toDouble(metricsMap.get("recall")));
                metrics.setAucRoc(toDouble(metricsMap.get("auc_roc")));
                builder.metrics(metrics);
            }

            // Parse nested champion_metrics object
            Object champObj = rawResponse.get("champion_metrics");
            if (champObj instanceof Map) {
                Map<String, Object> champMap = (Map<String, Object>) champObj;
                TrainResultDTO.ChampionMetrics champ = new TrainResultDTO.ChampionMetrics();
                champ.setAccuracy(toDouble(champMap.get("accuracy")));
                champ.setF1Score(toDouble(champMap.get("f1_score")));
                champ.setPrecisionScore(toDouble(champMap.get("precision_score")));
                champ.setRecallScore(toDouble(champMap.get("recall_score")));
                champ.setAucRoc(toDouble(champMap.get("auc_roc")));
                champ.setModelVersion((String) champMap.get("model_version"));
                builder.championMetrics(champ);
            }

            TrainResultDTO result = builder.build();

            // Cache for getMLOpsMetrics — guardamos la fecha real del entrenamiento
            if ("success".equals(result.getStatus())) {
                lastTrainResult = result;
                lastTrainResultDate = java.time.LocalDate.now().toString();
                System.out.println("---> Training result cached for MLOps metrics.");
            }

            return result;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("---> Python API not reachable: " + e.getMessage());
            return TrainResultDTO.builder()
                    .status("error")
                    .error("No se pudo conectar con la API de Python. Verifique que esté ejecutándose en "
                            + churnApiBaseUrl)
                    .build();
        } catch (Exception e) {
            System.err.println("---> Training error: " + e.getMessage());
            e.printStackTrace();
            return TrainResultDTO.builder()
                    .status("error")
                    .error("Error en auto-entrenamiento: " + e.getMessage())
                    .build();
        }
    }

    private Double toDouble(Object value) {
        if (value == null)
            return null;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Maps customer and account data to ChurnRequestDTO.
     * IMPORTANT: Translates IDs to descriptions for Python.
     */
    private ChurnRequestDTO mapToChurnRequest(Customer customer, AccountDetails accountDetails) {
        // Translate Geography (country)
        String geography = "Unknown";
        if (customer.getCountry() != null && customer.getCountry().getCountryDescription() != null) {
            geography = customer.getCountry().getCountryDescription();
        }

        // Translate Gender
        String gender = "Male"; // Default value
        if (customer.getGender() != null && customer.getGender().getGenderDescription() != null) {
            String genderDesc = customer.getGender().getGenderDescription().toLowerCase();
            if (genderDesc.contains("female") || genderDesc.contains("femenino") || genderDesc.startsWith("f")) {
                gender = "Female";
            }
        }

        return ChurnRequestDTO.builder()
                .creditScore(accountDetails.getCreditScore())
                .geography(geography)
                .gender(gender)
                .age(customer.getAge())
                .tenure(accountDetails.getTenure())
                .balance(accountDetails.getBalance() != null ? accountDetails.getBalance().doubleValue() : 0.0)
                .numOfProducts(accountDetails.getNumOfProducts())
                .hasCrCard(Boolean.TRUE.equals(accountDetails.getHasCrCard()) ? 1 : 0)
                .isActiveMember(Boolean.TRUE.equals(accountDetails.getIsActiveMember()) ? 1 : 0)
                .estimatedSalary(
                        accountDetails.getEstimatedSalary() != null ? accountDetails.getEstimatedSalary().doubleValue()
                                : 0.0)
                .exited(Boolean.TRUE.equals(accountDetails.getExited()) ? 1 : 0) // Map Ground Truth
                .build();
    }

    /**
     * Calls the Python API to get the prediction.
     */
    private ChurnResponseDTO callPythonApi(ChurnRequestDTO requestDTO) {
        try {
            String url = churnApiBaseUrl + "/churn/predict";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChurnRequestDTO> entity = new HttpEntity<>(requestDTO, headers);

            ChurnResponseDTO response = restTemplate.postForObject(url, entity, ChurnResponseDTO.class);

            // DEBUG LOGS
            if (response != null) {
                System.out.println("--> Python Response: Prob=" + response.getChurnProbability());
                System.out.println("--> Python Risk Factors: "
                        + (response.getRiskFactors() != null ? response.getRiskFactors().size() : "NULL"));
            }

            return response;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Python API unreachable — propagate as business exception so callers handle it
            // correctly
            System.err.println("[ChurnService] Python API not reachable: " + e.getMessage());
            throw new RuntimeException(
                    "No se pudo conectar con la API de predicción Python en " + churnApiBaseUrl +
                            ". Verifique que el servicio esté activo.",
                    e);
        } catch (Exception e) {
            System.err.println("[ChurnService] Error calling Python API: " + e.getMessage());
            throw new RuntimeException("Error al obtener predicción del modelo: " + e.getMessage(), e);
        }
    }

    /**
     * Calls the Python batch endpoint /churn/predict-batch with a chunk of
     * customers.
     * Each item in the chunk must have an 'id' field plus all ChurnInput fields.
     * Returns one result per customer — no SHAP explanations (batch use).
     */
    private List<ChurnBatchResultItemDTO> callPythonApiBatch(
            List<Long> customerIds,
            Map<Long, Customer> customerLookup,
            Map<Long, AccountDetails> accountMap) {

        List<Map<String, Object>> customers = new ArrayList<>();
        for (Long id : customerIds) {
            Customer c = customerLookup.get(id);
            AccountDetails acc = accountMap.get(id);
            if (c == null || acc == null)
                continue;

            ChurnRequestDTO req = mapToChurnRequest(c, acc);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("CreditScore", req.getCreditScore());
            item.put("Geography", req.getGeography());
            item.put("Gender", req.getGender());
            item.put("Age", req.getAge());
            item.put("Tenure", req.getTenure());
            item.put("Balance", req.getBalance());
            item.put("NumOfProducts", req.getNumOfProducts());
            item.put("HasCrCard", req.getHasCrCard());
            item.put("IsActiveMember", req.getIsActiveMember());
            item.put("EstimatedSalary", req.getEstimatedSalary());
            customers.add(item);
        }

        if (customers.isEmpty())
            return new ArrayList<>();

        Map<String, Object> requestBody = Map.of("customers", customers);
        String url = churnApiBaseUrl + "/churn/predict-batch";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ChurnBatchResultItemDTO[] response = restTemplate.postForObject(
                url, entity, ChurnBatchResultItemDTO[].class);

        return response != null ? Arrays.asList(response) : new ArrayList<>();
    }

    /**
     * Calculates geography statistics based on real customer data.
     * Uses batch queries to eliminate N+1 performance issues.
     */
    public List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> getGeographyStats() {
        // Load all customers for accurate geographic statistics
        List<Customer> customers = customerRepository.findAll();

        if (customers.isEmpty()) {
            return new ArrayList<>();
        }

        // ── BATCH QUERIES: 2 queries instead of N*2 ─────────────────────────
        List<Long> allIds = customers.stream()
                .map(Customer::getIdCustomer)
                .collect(Collectors.toList());

        Map<Long, AccountDetails> accountMap = new java.util.HashMap<>();
        for (AccountDetails ad : accountDetailsRepository.findByCustomerIds(allIds)) {
            accountMap.putIfAbsent(ad.getCustomer().getIdCustomer(), ad);
        }

        Map<Long, ChurnPredictions> predictionMap = new java.util.HashMap<>();
        for (ChurnPredictions cp : churnPredictionsRepository.findLatestByCustomerIds(allIds)) {
            predictionMap.putIfAbsent(cp.getCustomer().getIdCustomer(), cp);
        }

        // ── GROUP BY COUNTRY ─────────────────────────────────────────────────
        java.util.Map<String, List<Customer>> customersByCountry = customers.stream()
                .collect(java.util.stream.Collectors
                        .groupingBy(c -> (c.getCountry() != null && c.getCountry().getCountryDescription() != null)
                                ? c.getCountry().getCountryDescription()
                                : "Unknown"));

        List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> stats = new ArrayList<>();

        for (java.util.Map.Entry<String, List<Customer>> entry : customersByCountry.entrySet()) {
            String countryName = entry.getKey();
            List<Customer> countryCustomers = entry.getValue();

            int total = 0;
            int high = 0, medium = 0, low = 0;
            BigDecimal totalBalance = BigDecimal.ZERO;
            int churnCount = 0;

            // ── USE PRE-LOADED MAPS (no additional queries) ──────────────────
            for (Customer c : countryCustomers) {
                AccountDetails acc = accountMap.get(c.getIdCustomer());
                ChurnPredictions pred = predictionMap.get(c.getIdCustomer());

                // Solo incluimos si el cliente tiene datos financieros o predicción
                if (acc == null && pred == null)
                    continue;

                total++;
                if (acc != null && acc.getBalance() != null) {
                    totalBalance = totalBalance.add(acc.getBalance());
                }

                double riskVal = 0.0; // Default when no prediction exists
                if (pred != null) {
                    if (pred.getChurnProbability() != null) {
                        riskVal = pred.getChurnProbability().doubleValue();
                    }
                    if (Boolean.TRUE.equals(pred.getIsChurn()) || riskVal >= 0.45) {
                        churnCount++;
                    }
                }

                if (riskVal > 0.7)
                    high++;
                else if (riskVal >= 0.45)
                    medium++;
                else
                    low++;
            }

            if (total == 0)
                continue;

            BigDecimal avgBalance = totalBalance.divide(BigDecimal.valueOf(total), java.math.RoundingMode.HALF_UP);
            BigDecimal churnRate = BigDecimal.valueOf((double) churnCount * 100 / total);

            // Map country codes and flags
            String code = "UN";
            String flag = "🏳️";
            String cLower = countryName.toLowerCase();
            if (cLower.contains("fran")) {
                code = "FR";
                flag = "🇫🇷";
            } else if (cLower.contains("alem") || cLower.contains("germ")) {
                code = "DE";
                flag = "🇩🇪";
            } else if (cLower.contains("espa") || cLower.contains("spai")) {
                code = "ES";
                flag = "🇪🇸";
            }

            stats.add(com.naal.bankmind.dto.Churn.GeographyStatsDTO.builder()
                    .country(countryName)
                    .countryCode(code)
                    .flag(flag)
                    .totalCustomers(total)
                    .highRisk(high)
                    .mediumRisk(medium)
                    .lowRisk(low)
                    .avgBalance(avgBalance)
                    .churnRate(churnRate)
                    .build());
        }

        return stats;
    }

    /**
     * Gets MLOps metrics.
     * Priority: 1) In-memory cache from latest training, 2) DB
     * (churn_training_history), 3) static defaults.
     */
    public com.naal.bankmind.dto.Churn.MLOpsMetricsDTO getMLOpsMetrics() {
        long totalPredictions = churnPredictionsRepository.count();

        // Priority 1: Use cached metrics from last training run (in this JVM session)
        if (lastTrainResult != null && lastTrainResult.getMetrics() != null) {
            TrainResultDTO.TrainMetrics m = lastTrainResult.getMetrics();
            return com.naal.bankmind.dto.Churn.MLOpsMetricsDTO.builder()
                    .modelStatus("Active")
                    .modelVersion(lastTrainResult.getRunId() != null
                            ? "run-" + lastTrainResult.getRunId().substring(0,
                                    Math.min(8, lastTrainResult.getRunId().length()))
                            : "v-latest")
                    .totalPredictions(totalPredictions)
                    .lastTrainingDate(
                            lastTrainResultDate != null ? lastTrainResultDate : java.time.LocalDate.now().toString())
                    .precision(m.getPrecision() != null ? m.getPrecision() * 100 : 0.0)
                    .recall(m.getRecall() != null ? m.getRecall() * 100 : 0.0)
                    .f1Score(m.getF1Score() != null ? m.getF1Score() * 100 : 0.0)
                    .aucRoc(m.getAucRoc() != null ? m.getAucRoc() * 100 : 0.0)
                    .build();
        }

        // Priority 2: Read metrics from churn_training_history table.
        // Prefer the champion (in-production) model with the highest AUC-ROC — it
        // represents
        // the best-performing model currently serving predictions. Fall back to latest
        // record.
        Optional<ChurnTrainingHistory> champion = churnTrainingHistoryRepository
                .findTopByInProductionTrueOrderByAucRocDesc();
        Optional<ChurnTrainingHistory> fallback = churnTrainingHistoryRepository
                .findTopByOrderByTrainingDateDesc();
        Optional<ChurnTrainingHistory> selected = champion.isPresent() ? champion : fallback;

        if (selected.isPresent()) {
            ChurnTrainingHistory h = selected.get();
            return com.naal.bankmind.dto.Churn.MLOpsMetricsDTO.builder()
                    .modelStatus("Active")
                    .modelVersion(h.getModelVersion() != null ? h.getModelVersion() : "v-latest")
                    .totalPredictions(totalPredictions)
                    .lastTrainingDate(h.getTrainingDate() != null
                            ? h.getTrainingDate().toLocalDate().toString()
                            : java.time.LocalDate.now().toString())
                    .precision(h.getPrecisionScore() != null ? h.getPrecisionScore() * 100 : 0.0)
                    .recall(h.getRecallScore() != null ? h.getRecallScore() * 100 : 0.0)
                    .f1Score(h.getF1Score() != null ? h.getF1Score() * 100 : 0.0)
                    .aucRoc(h.getAucRoc() != null ? h.getAucRoc() * 100 : 0.0)
                    .build();
        }

        // Priority 3: Base model metrics — shown before the first local training run.
        // These reflect the pre-trained XGBoost baseline shipped with the system.
        return com.naal.bankmind.dto.Churn.MLOpsMetricsDTO.builder()
                .modelStatus("Active")
                .modelVersion("v1.0-base")
                .totalPredictions(totalPredictions)
                .lastTrainingDate("—")
                .precision(92.3)
                .recall(89.1)
                .f1Score(90.6)
                .aucRoc(95.4)
                .build();
    }

    // ============================================================
    // CAMPAIGN MANAGEMENT (M2 — Real Persistence)
    // ============================================================

    /**
     * Gets all real campaigns from the database (excludes the "Interacciones
     * Individuales"
     * pseudo-campaign created by logInteraction), ordered by start date descending.
     * Uses a single GROUP BY query to resolve convertedCount — avoids N+1.
     */
    public List<CampaignLogDTO> getCampaigns() {
        List<CampaignLog> campaigns = campaignLogRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "startDate"))
                .stream()
                .filter(c -> !"Interacciones Individuales".equals(c.getName()))
                .collect(Collectors.toList());

        // Build convertedCount map in one query
        Map<Long, Integer> convertedMap = new java.util.HashMap<>();
        campaignTargetRepository.countConvertedGroupedByCampaign()
                .forEach(row -> convertedMap.put((Long) row[0], ((Number) row[1]).intValue()));

        return campaigns.stream().map(c -> CampaignLogDTO.builder()
                .id(c.getIdCampaign())
                .name(c.getName())
                .segmentName(c.getSegment() != null ? c.getSegment().getName() : "Sin segmento")
                .strategyName(c.getStrategy() != null ? c.getStrategy().getName() : "Sin estrategia")
                .startDate(c.getStartDate() != null ? c.getStartDate().toLocalDate().toString() : "")
                .status(c.getStatus())
                .budgetAllocated(c.getBudgetAllocated() != null ? c.getBudgetAllocated().doubleValue() : 0.0)
                .expectedRoi(c.getExpectedRoi() != null ? c.getExpectedRoi().doubleValue() : 0.0)
                .targetedCount(c.getTargetedCount() != null ? c.getTargetedCount() : 0)
                .convertedCount(convertedMap.getOrDefault(c.getIdCampaign(), 0))
                .build()).collect(Collectors.toList());
    }

    /**
     * Creates a new campaign and persists it in the database.
     * Also creates campaign_target entries for each target customer.
     */
    public CampaignLogDTO createCampaign(CampaignLogDTO req) {
        CampaignLog campaign = new CampaignLog();
        campaign.setName(req.getName());
        campaign.setStatus("ACTIVE");
        campaign.setStartDate(LocalDateTime.now());
        campaign.setBudgetAllocated(req.getBudget() != null
                ? BigDecimal.valueOf(req.getBudget())
                : BigDecimal.ZERO);
        campaign.setExpectedRoi(req.getExpectedRoi() != null
                ? BigDecimal.valueOf(req.getExpectedRoi())
                : BigDecimal.ZERO);
        int targetedCount = (req.getTargetedCount() != null && req.getTargetedCount() > 0)
                ? req.getTargetedCount()
                : (req.getTargets() != null ? req.getTargets().size() : 0);
        campaign.setTargetedCount(targetedCount);
        campaign.setConvertedCount(0);

        // Set FK relationships
        if (req.getStrategyId() != null) {
            retentionStrategyDefRepository.findById(req.getStrategyId())
                    .ifPresent(campaign::setStrategy);
        }
        if (req.getSegmentId() != null) {
            retentionSegmentDefRepository.findById(req.getSegmentId())
                    .ifPresent(campaign::setSegment);
        }

        campaign = campaignLogRepository.save(campaign);

        // Create campaign_target entries for targeted customers
        if (req.getTargets() != null && !req.getTargets().isEmpty()) {
            // Explicit target list provided
            for (Long customerId : req.getTargets()) {
                try {
                    CampaignTarget target = new CampaignTarget();
                    CampaignTargetKey key = new CampaignTargetKey();
                    key.setIdCampaign(campaign.getIdCampaign());
                    key.setIdCustomer(customerId);
                    target.setId(key);
                    target.setStatus("TARGETED");
                    campaignTargetRepository.save(target);
                } catch (Exception e) {
                    System.err.println("Error adding target customer " + customerId + ": " + e.getMessage());
                }
            }
        } else if (campaign.getSegment() != null) {
            // Auto-assign: find customers matching segment rules from latest predictions
            List<java.util.Map<String, Object>> rules = new ArrayList<>();
            try {
                String rulesJson = campaign.getSegment().getRulesJson();
                if (rulesJson != null && !rulesJson.isEmpty()) {
                    rules = objectMapper.readValue(rulesJson, new TypeReference<List<java.util.Map<String, Object>>>() {
                    });
                }
            } catch (Exception e) {
                System.err.println("Error parsing segment rules: " + e.getMessage());
            }

            List<ChurnPredictions> latestPredictions = churnPredictionsRepository.findLatestForAllCustomers();
            List<Long> customerIds = latestPredictions.stream()
                    .map(p -> p.getCustomer().getIdCustomer())
                    .collect(Collectors.toList());

            java.util.Map<Long, com.naal.bankmind.entity.AccountDetails> accMap = new java.util.HashMap<>();
            if (!customerIds.isEmpty()) {
                accountDetailsRepository.findByCustomerIds(customerIds)
                        .forEach(ad -> accMap.putIfAbsent(ad.getCustomer().getIdCustomer(), ad));
            }

            final List<java.util.Map<String, Object>> finalRules = rules;
            int matched = 0;
            for (ChurnPredictions pred : latestPredictions) {
                Customer c = pred.getCustomer();
                com.naal.bankmind.entity.AccountDetails acc = accMap.get(c.getIdCustomer());
                if (matchesSegmentRules(finalRules, pred, c, acc)) {
                    try {
                        CampaignTarget target = new CampaignTarget();
                        CampaignTargetKey key = new CampaignTargetKey();
                        key.setIdCampaign(campaign.getIdCampaign());
                        key.setIdCustomer(c.getIdCustomer());
                        target.setId(key);
                        target.setStatus("TARGETED");
                        campaignTargetRepository.save(target);
                        matched++;
                    } catch (Exception e) {
                        System.err.println("Error adding auto-target " + c.getIdCustomer() + ": " + e.getMessage());
                    }
                }
            }
            // Update targetedCount with real match count
            campaign.setTargetedCount(matched);
            campaignLogRepository.save(campaign);
            targetedCount = matched;
        }

        // Return DTO with resolved names
        return CampaignLogDTO.builder()
                .id(campaign.getIdCampaign())
                .name(campaign.getName())
                .segmentName(campaign.getSegment() != null ? campaign.getSegment().getName() : "Segmento Personalizado")
                .strategyName(campaign.getStrategy() != null ? campaign.getStrategy().getName() : "Estrategia")
                .startDate(campaign.getStartDate().toLocalDate().toString())
                .status(campaign.getStatus())
                .budgetAllocated(
                        campaign.getBudgetAllocated() != null ? campaign.getBudgetAllocated().doubleValue() : 0.0)
                .expectedRoi(req.getExpectedRoi() != null ? req.getExpectedRoi() : 0.0)
                .targetedCount(targetedCount)
                .convertedCount(0)
                .build();
    }

    /**
     * Deletes a campaign and its associated campaign_target records.
     */
    public void deleteCampaign(Long id) {
        CampaignLog campaign = campaignLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        List<CampaignTarget> targets = campaignTargetRepository.findByIdIdCampaign(id);
        campaignTargetRepository.deleteAll(targets);
        campaignLogRepository.delete(campaign);
    }

    /**
     * Updates the status of a campaign (ACTIVE → COMPLETED or CANCELLED).
     * Valid statuses: ACTIVE, COMPLETED, CANCELLED
     */
    public CampaignLogDTO updateCampaignStatus(Long id, String status) {
        CampaignLog campaign = campaignLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        campaign.setStatus(status);
        campaignLogRepository.save(campaign);
        Map<Long, Integer> convertedMap = new java.util.HashMap<>();
        campaignTargetRepository.countConvertedGroupedByCampaign()
                .forEach(row -> convertedMap.put((Long) row[0], ((Number) row[1]).intValue()));
        return CampaignLogDTO.builder()
                .id(campaign.getIdCampaign())
                .name(campaign.getName())
                .segmentName(campaign.getSegment() != null ? campaign.getSegment().getName() : "Sin segmento")
                .strategyName(campaign.getStrategy() != null ? campaign.getStrategy().getName() : "Sin estrategia")
                .startDate(campaign.getStartDate() != null ? campaign.getStartDate().toLocalDate().toString() : "")
                .status(campaign.getStatus())
                .budgetAllocated(
                        campaign.getBudgetAllocated() != null ? campaign.getBudgetAllocated().doubleValue() : 0.0)
                .expectedRoi(campaign.getExpectedRoi() != null ? campaign.getExpectedRoi().doubleValue() : 0.0)
                .targetedCount(campaign.getTargetedCount() != null ? campaign.getTargetedCount() : 0)
                .convertedCount(convertedMap.getOrDefault(campaign.getIdCampaign(), 0))
                .build();
    }

    /**
     * Counts how many customers would match a segment's rules, without creating a
     * campaign.
     * Used for the "preview" shown in the campaign creation modal.
     */
    public int previewSegmentCount(Long segmentId) {
        RetentionSegmentDef segment = retentionSegmentDefRepository.findById(segmentId.intValue())
                .orElseThrow(() -> new RuntimeException("Segment not found: " + segmentId));

        List<java.util.Map<String, Object>> rules = new ArrayList<>();
        try {
            String rulesJson = segment.getRulesJson();
            if (rulesJson != null && !rulesJson.isEmpty()) {
                rules = objectMapper.readValue(rulesJson, new TypeReference<List<java.util.Map<String, Object>>>() {
                });
            }
        } catch (Exception e) {
            System.err.println("Error parsing segment rules for preview: " + e.getMessage());
        }

        List<ChurnPredictions> latestPredictions = churnPredictionsRepository.findLatestForAllCustomers();
        List<Long> customerIds = latestPredictions.stream()
                .map(p -> p.getCustomer().getIdCustomer())
                .collect(Collectors.toList());

        java.util.Map<Long, com.naal.bankmind.entity.AccountDetails> accMap = new java.util.HashMap<>();
        if (!customerIds.isEmpty()) {
            accountDetailsRepository.findByCustomerIds(customerIds)
                    .forEach(ad -> accMap.putIfAbsent(ad.getCustomer().getIdCustomer(), ad));
        }

        final List<java.util.Map<String, Object>> finalRules = rules;
        int count = 0;
        for (ChurnPredictions pred : latestPredictions) {
            Customer c = pred.getCustomer();
            com.naal.bankmind.entity.AccountDetails acc = accMap.get(c.getIdCustomer());
            if (matchesSegmentRules(finalRules, pred, c, acc)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Evaluates segment rules against a customer's prediction and account data.
     * Returns true if ALL rules pass (AND logic).
     */
    private boolean matchesSegmentRules(
            List<java.util.Map<String, Object>> rules,
            ChurnPredictions pred,
            Customer customer,
            com.naal.bankmind.entity.AccountDetails acc) {
        for (java.util.Map<String, Object> rule : rules) {
            String field = (String) rule.get("field");
            String op = (String) rule.get("op");
            Object valObj = rule.get("val");
            if (field == null || op == null || valObj == null)
                continue;

            // Numeric fields
            Double numVal = null;
            switch (field) {
                case "churn_prob":
                    if (pred.getChurnProbability() != null)
                        numVal = pred.getChurnProbability().doubleValue();
                    break;
                case "balance":
                    if (acc != null && acc.getBalance() != null)
                        numVal = acc.getBalance().doubleValue();
                    break;
                case "age":
                    if (customer.getAge() != null)
                        numVal = customer.getAge().doubleValue();
                    break;
                case "products":
                    if (acc != null && acc.getNumOfProducts() != null)
                        numVal = acc.getNumOfProducts().doubleValue();
                    break;
                case "score":
                    if (acc != null && acc.getCreditScore() != null)
                        numVal = acc.getCreditScore().doubleValue();
                    break;
                case "risk":
                    // Frontend sends risk as 0-100 percentage
                    if (pred.getChurnProbability() != null)
                        numVal = pred.getChurnProbability().doubleValue() * 100;
                    break;
                default:
                    break;
            }

            // String fields
            String strVal = null;
            switch (field) {
                case "job":
                    strVal = customer.getJob();
                    break;
                case "risk_level":
                    strVal = pred.getRiskLevel();
                    break;
                case "country":
                    if (customer.getCountry() != null && customer.getCountry().getCountryDescription() != null)
                        strVal = customer.getCountry().getCountryDescription();
                    break;
                default:
                    break;
            }

            if (numVal != null) {
                double target;
                try {
                    target = Double.parseDouble(valObj.toString());
                } catch (NumberFormatException e) {
                    continue;
                }
                boolean ok;
                switch (op) {
                    case ">":
                        ok = numVal > target;
                        break;
                    case "<":
                        ok = numVal < target;
                        break;
                    case ">=":
                        ok = numVal >= target;
                        break;
                    case "<=":
                        ok = numVal <= target;
                        break;
                    case "=":
                    case "==":
                        ok = numVal.equals(target);
                        break;
                    case "!=":
                        ok = !numVal.equals(target);
                        break;
                    default:
                        ok = true;
                }
                if (!ok)
                    return false;
            } else if (strVal != null) {
                String target = valObj.toString();
                boolean ok;
                switch (op) {
                    case "=":
                    case "==":
                        ok = strVal.equalsIgnoreCase(target);
                        break;
                    case "!=":
                        ok = !strVal.equalsIgnoreCase(target);
                        break;
                    default:
                        ok = true;
                }
                if (!ok)
                    return false;
            }
            // Unknown field → skip (don't block)
        }
        return true;
    }

    /**
     * Returns all campaign targets for a given campaign, with customer name
     * resolved.
     */
    public List<com.naal.bankmind.dto.Churn.CampaignTargetDTO> getCampaignTargets(Long campaignId) {
        return campaignTargetRepository.findByIdIdCampaign(campaignId).stream()
                .map(t -> {
                    com.naal.bankmind.entity.Customer c = customerRepository.findById(t.getId().getIdCustomer())
                            .orElse(null);
                    String name = c != null
                            ? ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                                    + (c.getSurname() != null ? c.getSurname() : "")).trim()
                            : "Cliente #" + t.getId().getIdCustomer();
                    return com.naal.bankmind.dto.Churn.CampaignTargetDTO.builder()
                            .customerId(t.getId().getIdCustomer())
                            .customerName(name)
                            .status(t.getStatus())
                            .contactDate(
                                    t.getContactDate() != null ? t.getContactDate().toLocalDate().toString() : null)
                            .responseDate(
                                    t.getResponseDate() != null ? t.getResponseDate().toLocalDate().toString() : null)
                            .build();
                }).collect(Collectors.toList());
    }

    /**
     * Updates the status of a campaign target.
     * Automatically sets contactDate when status becomes CONTACTED,
     * and responseDate when status becomes CONVERTED or FAILED.
     */
    public void updateCampaignTargetStatus(Long campaignId, Long customerId, String status) {
        CampaignTargetKey key = new CampaignTargetKey();
        key.setIdCampaign(campaignId);
        key.setIdCustomer(customerId);
        CampaignTarget target = campaignTargetRepository.findById(key)
                .orElseThrow(() -> new RuntimeException(
                        "Target not found: campaign=" + campaignId + " customer=" + customerId));
        target.setStatus(status);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if ("CONTACTED".equals(status) && target.getContactDate() == null) {
            target.setContactDate(now);
        } else if ("CONVERTED".equals(status) || "FAILED".equals(status)) {
            if (target.getContactDate() == null)
                target.setContactDate(now);
            target.setResponseDate(now);
        }
        campaignTargetRepository.save(target);
    }

    /**
     * Gets all segment definitions from the database.
     * Converts the JSON rules to DTO format expected by frontend.
     */
    public List<SegmentDTO> getAllSegments() {
        List<RetentionSegmentDef> segments = retentionSegmentDefRepository.findAll();
        List<SegmentDTO> result = new ArrayList<>();

        for (RetentionSegmentDef seg : segments) {
            try {
                List<SegmentDTO.RuleDTO> rules = new ArrayList<>();
                if (seg.getRulesJson() != null && !seg.getRulesJson().isEmpty()) {
                    List<java.util.Map<String, Object>> rawRules = objectMapper.readValue(
                            seg.getRulesJson(),
                            new TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                    for (java.util.Map<String, Object> r : rawRules) {
                        rules.add(SegmentDTO.RuleDTO.builder()
                                .field((String) r.get("field"))
                                .op((String) r.get("op"))
                                .val(r.get("val"))
                                .build());
                    }
                }

                result.add(SegmentDTO.builder()
                        .id(seg.getIdSegment())
                        .name(seg.getName())
                        .description(seg.getDescription())
                        .rules(rules)
                        .build());
            } catch (Exception e) {
                System.err.println("Error parsing segment rules for ID " + seg.getIdSegment() + ": " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Gets all active retention strategies from the database.
     */
    public List<RetentionStrategyDef> getAllStrategies() {
        return retentionStrategyDefRepository.findByIsActiveTrue();
    }

    /**
     * Creates a new custom segment definition.
     * Converts the DTO rules into a JSONB string and persists to DB.
     *
     * @param dto The segment definition with rules
     * @return The created segment as DTO (with generated ID)
     */
    public SegmentDTO createSegment(SegmentDTO dto) {
        RetentionSegmentDef entity = new RetentionSegmentDef();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCreatedAt(LocalDateTime.now());

        // Convert rules list to JSON string for JSONB column
        try {
            if (dto.getRules() != null && !dto.getRules().isEmpty()) {
                entity.setRulesJson(objectMapper.writeValueAsString(dto.getRules()));
            } else {
                entity.setRulesJson("[]");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error serializing segment rules to JSON: " + e.getMessage());
        }

        RetentionSegmentDef saved = retentionSegmentDefRepository.save(entity);

        return SegmentDTO.builder()
                .id(saved.getIdSegment())
                .name(saved.getName())
                .description(saved.getDescription())
                .rules(dto.getRules())
                .build();
    }

    /**
     * Deletes a segment definition by ID.
     *
     * @param id The segment ID to delete
     * @throws RuntimeException if segment not found
     */
    public void deleteSegment(Integer id) {
        if (id != null && id <= 4) {
            throw new RuntimeException("System segments (IDs 1-4) cannot be deleted.");
        }
        if (!retentionSegmentDefRepository.existsById(id)) {
            throw new RuntimeException("Segment not found with ID: " + id);
        }
        retentionSegmentDefRepository.deleteById(id);
    }

    // ============================================================
    // PERFORMANCE MONITOR PROXY
    // ============================================================

    /**
     * Gets the current performance monitor status from the Python API.
     * Proxies GET /churn/monitor/status
     */
    public PerformanceStatusDTO getPerformanceStatus() {
        try {
            String url = churnApiBaseUrl + "/churn/monitor/status";
            System.out.println("---> Calling Python Monitor API: GET " + url);

            Map<String, Object> rawResponse = restTemplate.getForObject(url, Map.class);

            if (rawResponse == null) {
                return PerformanceStatusDTO.builder()
                        .status("error")
                        .message("La API de Python no devolvió respuesta.")
                        .build();
            }

            System.out.println("---> Monitor Status Response: " + rawResponse);
            return mapToPerformanceStatus(rawResponse);

        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("---> Python API not reachable: " + e.getMessage());
            return PerformanceStatusDTO.builder()
                    .status("error")
                    .message("No se pudo conectar con la API de Python. Verifique que esté ejecutándose en "
                            + churnApiBaseUrl)
                    .build();
        } catch (Exception e) {
            System.err.println("---> Monitor status error: " + e.getMessage());
            return PerformanceStatusDTO.builder()
                    .status("error")
                    .message("Error consultando estado del monitor: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Manually triggers a performance evaluation via the Python API.
     * Proxies POST /churn/monitor/evaluate
     */
    public PerformanceStatusDTO triggerPerformanceEvaluation() {
        try {
            String url = churnApiBaseUrl + "/churn/monitor/evaluate";
            System.out.println("---> Calling Python Monitor API: POST " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            Map<String, Object> rawResponse = restTemplate.postForObject(url, entity, Map.class);

            if (rawResponse == null) {
                return PerformanceStatusDTO.builder()
                        .status("error")
                        .message("La API de Python no devolvió respuesta.")
                        .build();
            }

            System.out.println("---> Evaluation Response: " + rawResponse);
            return mapToPerformanceStatus(rawResponse);

        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("---> Python API not reachable: " + e.getMessage());
            return PerformanceStatusDTO.builder()
                    .status("error")
                    .message("No se pudo conectar con la API de Python. Verifique que esté ejecutándose en "
                            + churnApiBaseUrl)
                    .build();
        } catch (Exception e) {
            System.err.println("---> Evaluation error: " + e.getMessage());
            return PerformanceStatusDTO.builder()
                    .status("error")
                    .message("Error al evaluar rendimiento: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Calculates high-level executive metrics for the CEO Dashboard.
     * Focuses on monetary impact, ROI of retention, and strategic insights.
     */
    public Map<String, Object> getExecutiveBusinessMetrics() {
        // 1. Todos los clientes con predicción disponible (portafolio real analizado).
        // Partimos de churn_predictions → un query trae predicción + customer en JOIN.
        List<ChurnPredictions> allLatest = churnPredictionsRepository.findLatestForAllCustomers();

        if (allLatest.isEmpty()) {
            Map<String, Object> empty = new java.util.HashMap<>();
            empty.put("totalAnalyzed", 0L);
            empty.put("highRiskCount", 0L);
            empty.put("mediumRiskCount", 0L);
            empty.put("lowRiskCount", 0L);
            empty.put("totalCampaigns", 0);
            empty.put("totalTargeted", 0);
            empty.put("totalConverted", 0);
            empty.put("totalInvestment", BigDecimal.ZERO);
            empty.put("strategicInsights", new ArrayList<>());
            return empty;
        }

        List<Customer> customers = allLatest.stream()
                .map(ChurnPredictions::getCustomer)
                .collect(Collectors.toList());
        List<Long> ids = customers.stream().map(Customer::getIdCustomer).collect(Collectors.toList());

        Map<Long, AccountDetails> accounts = new java.util.HashMap<>();
        accountDetailsRepository.findByCustomerIds(ids).forEach(a -> accounts.put(a.getCustomer().getIdCustomer(), a));

        // Predicciones ya cargadas desde el JOIN — mapear por customer ID
        Map<Long, ChurnPredictions> predictions = allLatest.stream()
                .collect(Collectors.toMap(p -> p.getCustomer().getIdCustomer(), p -> p));

        // 2. Aggregate Business KPIs + Strategic Insights counters
        BigDecimal investmentCost = BigDecimal.ZERO;

        // Risk profile distribution (all analyzed customers)
        long totalAnalyzed = 0;
        long highRiskCount = 0; // > 70%
        long mediumRiskCount = 0; // 45–70%
        long lowRiskCount = 0; // < 45%

        // Counters for strategic insights (customers with churn risk > 0.45)
        long totalAtRisk = 0;
        long inactiveAtRisk = 0;
        long monoProductAtRisk = 0;
        long seniorAtRisk = 0; // age > 45
        long lowScoreAtRisk = 0; // credit_score < 550
        java.util.Map<String, Long> countryAtRisk = new java.util.HashMap<>();

        for (Customer c : customers) {
            AccountDetails acc = accounts.get(c.getIdCustomer());
            ChurnPredictions pred = predictions.get(c.getIdCustomer());

            if (acc == null || pred == null)
                continue;

            double risk = pred.getChurnProbability() != null ? pred.getChurnProbability().doubleValue() : 0.0;
            int riskPct = (int) Math.round(risk * 100);

            // Risk profile distribution
            totalAnalyzed++;
            if (riskPct > 70)
                highRiskCount++;
            else if (riskPct >= 45)
                mediumRiskCount++;
            else
                lowRiskCount++;

            // Strategic insights: only count customers with meaningful churn risk
            if (risk > 0.45) {
                totalAtRisk++;

                if (Boolean.FALSE.equals(acc.getIsActiveMember()))
                    inactiveAtRisk++;

                if (acc.getNumOfProducts() != null && acc.getNumOfProducts() == 1)
                    monoProductAtRisk++;

                if (c.getAge() != null && c.getAge() > 45)
                    seniorAtRisk++;

                if (acc.getCreditScore() != null && acc.getCreditScore() < 550)
                    lowScoreAtRisk++;

                String country = (c.getCountry() != null && c.getCountry().getCountryDescription() != null)
                        ? c.getCountry().getCountryDescription()
                        : "Otros";
                countryAtRisk.merge(country, 1L, Long::sum);
            }
        }

        // 3. Campaign Summary — excludes the "Interacciones Individuales"
        // pseudo-campaign
        // created internally by logInteraction() to track individual advisor actions.
        List<CampaignLog> activeCampaigns = campaignLogRepository.findAll().stream()
                .filter(c -> !"Interacciones Individuales".equals(c.getName()))
                .collect(Collectors.toList());
        int totalCampaigns = activeCampaigns.size();
        int totalTargeted = 0;
        int totalConverted = campaignTargetRepository.countAllConvertedExcluding("Interacciones Individuales");
        for (CampaignLog camp : activeCampaigns) {
            investmentCost = investmentCost
                    .add(camp.getBudgetAllocated() != null ? camp.getBudgetAllocated() : BigDecimal.ZERO);
            totalTargeted += camp.getTargetedCount() != null ? camp.getTargetedCount() : 0;
        }

        // 4. Strategic Insights — derived from real customer data
        List<Map<String, Object>> strategicInsights = new ArrayList<>();

        if (totalAtRisk > 0) {
            long fTotal = totalAtRisk;

            // Insight 1: Inactividad Operativa
            int inactivePct = (int) Math.round((double) inactiveAtRisk / fTotal * 100);
            strategicInsights.add(java.util.Map.of(
                    "cause", "Inactividad Operativa",
                    "segment", "Miembros Inactivos",
                    "impact", inactivePct >= 40 ? "ALTO" : inactivePct >= 20 ? "MEDIO" : "BAJO",
                    "pct", inactivePct));

            // Insight 2: Baja Vinculación de Productos
            int monoProductPct = (int) Math.round((double) monoProductAtRisk / fTotal * 100);
            strategicInsights.add(java.util.Map.of(
                    "cause", "Baja Vinculación de Productos",
                    "segment", "Solo 1 Producto",
                    "impact", monoProductPct >= 40 ? "ALTO" : monoProductPct >= 20 ? "MEDIO" : "BAJO",
                    "pct", monoProductPct));

            // Insight 3: País con mayor concentración de riesgo
            countryAtRisk.entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue())
                    .ifPresent(e -> {
                        int countryPct = (int) Math.round((double) e.getValue() / fTotal * 100);
                        strategicInsights.add(java.util.Map.of(
                                "cause", "Concentración Geográfica",
                                "segment", e.getKey(),
                                "impact", countryPct >= 40 ? "ALTO" : "MEDIO",
                                "pct", countryPct));
                    });

            // Insight 4: Perfil Senior
            int seniorPct = (int) Math.round((double) seniorAtRisk / fTotal * 100);
            strategicInsights.add(java.util.Map.of(
                    "cause", "Perfil Demográfico Senior",
                    "segment", "Mayores de 45 años",
                    "impact", seniorPct >= 40 ? "ALTO" : seniorPct >= 20 ? "MEDIO" : "BAJO",
                    "pct", seniorPct));

            // Insight 5: Score crediticio bajo (solo si hay casos significativos)
            if (lowScoreAtRisk > 0) {
                int lowScorePct = (int) Math.round((double) lowScoreAtRisk / fTotal * 100);
                strategicInsights.add(java.util.Map.of(
                        "cause", "Deterioro Crediticio",
                        "segment", "Score < 550",
                        "impact", lowScorePct >= 30 ? "ALTO" : lowScorePct >= 15 ? "MEDIO" : "BAJO",
                        "pct", lowScorePct));
            }

            // Sort by pct descending — most impactful first
            strategicInsights.sort((a, b) -> Integer.compare((Integer) b.get("pct"), (Integer) a.get("pct")));
        }

        // 5. Final Payload for Executive View
        Map<String, Object> metrics = new java.util.HashMap<>();
        // Risk profile distribution
        metrics.put("totalAnalyzed", totalAnalyzed);
        metrics.put("highRiskCount", highRiskCount);
        metrics.put("mediumRiskCount", mediumRiskCount);
        metrics.put("lowRiskCount", lowRiskCount);
        // Campaign summary (real data)
        metrics.put("totalCampaigns", totalCampaigns);
        metrics.put("totalTargeted", totalTargeted);
        metrics.put("totalConverted", totalConverted);
        metrics.put("totalInvestment", investmentCost.setScale(2, java.math.RoundingMode.HALF_UP));
        // Strategic insights
        metrics.put("strategicInsights", strategicInsights);

        return metrics;
    }

    // ============================================================
    // MLOPS ANALYTICS CHARTS
    // ============================================================

    /**
     * Returns the last 30 training/evaluation events ordered chronologically.
     * Used by the "Evolución de Métricas" and "Historial de Entrenamientos" charts.
     * Metrics are converted to 0-100 scale for direct frontend display.
     */
    public List<TrainingHistoryPointDTO> getTrainingEvolution() {
        List<ChurnTrainingHistory> raw = churnTrainingHistoryRepository.findTop30ByOrderByTrainingDateDesc();
        Collections.reverse(raw); // Ascending chronological order for the chart

        return raw.stream().map(h -> {
            String dateLabel = h.getTrainingDate() != null
                    ? h.getTrainingDate().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
                    : "N/A";

            return TrainingHistoryPointDTO.builder()
                    .date(dateLabel)
                    .triggerReason(h.getTriggerReason())
                    .recall(h.getRecallScore() != null ? round2(h.getRecallScore() * 100) : null)
                    .precision(h.getPrecisionScore() != null ? round2(h.getPrecisionScore() * 100) : null)
                    .f1Score(h.getF1Score() != null ? round2(h.getF1Score() * 100) : null)
                    .accuracy(h.getAccuracy() != null ? round2(h.getAccuracy() * 100) : null)
                    .aucRoc(h.getAucRoc() != null ? round2(h.getAucRoc() * 100) : null)
                    .inProduction(h.getInProduction())
                    .evaluatedSamples(h.getEvaluatedSamples())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Returns the churn probability distribution grouped into 10 buckets (0-10%,
     * 10-20%, ..., 90-100%).
     * Used by the "Distribución de Probabilidades" histogram.
     */
    public List<PredictionBucketDTO> getPredictionDistribution() {
        // Use latest prediction per customer — avoids inflating buckets with repeated
        // analyses
        List<java.math.BigDecimal> probabilities = churnPredictionsRepository.findLatestChurnProbabilitiesPerCustomer();

        int[] counts = new int[10];
        for (java.math.BigDecimal prob : probabilities) {
            if (prob != null) {
                int bucket = (int) Math.min(prob.doubleValue() * 10, 9);
                counts[bucket]++;
            }
        }

        List<PredictionBucketDTO> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            result.add(PredictionBucketDTO.builder()
                    .bucket((i * 10) + "-" + ((i + 1) * 10) + "%")
                    .count(counts[i])
                    .build());
        }
        return result;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Maps the raw Python API response to a PerformanceStatusDTO.
     * Handles snake_case to camelCase conversion.
     */
    @SuppressWarnings("unchecked")
    private PerformanceStatusDTO mapToPerformanceStatus(Map<String, Object> raw) {
        PerformanceStatusDTO.PerformanceStatusDTOBuilder builder = PerformanceStatusDTO.builder()
                .status((String) raw.getOrDefault("status", "unknown"))
                .message((String) raw.get("message"));

        // Metrics
        if (raw.get("recall") != null)
            builder.recall(toDouble(raw.get("recall")));
        if (raw.get("f1_score") != null)
            builder.f1Score(toDouble(raw.get("f1_score")));
        if (raw.get("precision") != null)
            builder.precision(toDouble(raw.get("precision")));
        if (raw.get("accuracy") != null)
            builder.accuracy(toDouble(raw.get("accuracy")));
        if (raw.get("recall_threshold") != null)
            builder.recallThreshold(toDouble(raw.get("recall_threshold")));

        // Sample counts
        if (raw.get("evaluated_samples") != null)
            builder.evaluatedSamples(((Number) raw.get("evaluated_samples")).intValue());
        if (raw.get("min_samples_required") != null)
            builder.minSamplesRequired(((Number) raw.get("min_samples_required")).intValue());
        if (raw.get("maturation_days") != null)
            builder.maturationDays(((Number) raw.get("maturation_days")).intValue());

        // Monitor config
        if (raw.get("monitor_enabled") != null)
            builder.monitorEnabled((Boolean) raw.get("monitor_enabled"));
        if (raw.get("monitor_interval_hours") != null)
            builder.monitorIntervalHours(((Number) raw.get("monitor_interval_hours")).intValue());

        // Dates
        builder.lastEvaluationDate((String) raw.get("last_evaluation_date"));
        builder.nextEvaluationDate((String) raw.get("next_evaluation_date"));

        // Auto-training info
        if (raw.get("auto_training_triggered") != null)
            builder.autoTrainingTriggered((Boolean) raw.get("auto_training_triggered"));
        builder.triggerReason((String) raw.get("trigger_reason"));

        // Confusion matrix
        if (raw.get("true_positives") != null)
            builder.truePositives(((Number) raw.get("true_positives")).intValue());
        if (raw.get("false_positives") != null)
            builder.falsePositives(((Number) raw.get("false_positives")).intValue());
        if (raw.get("true_negatives") != null)
            builder.trueNegatives(((Number) raw.get("true_negatives")).intValue());
        if (raw.get("false_negatives") != null)
            builder.falseNegatives(((Number) raw.get("false_negatives")).intValue());

        // Training result
        builder.trainingRunId((String) raw.get("training_run_id"));
        builder.trainingError((String) raw.get("training_error"));

        return builder.build();
    }

    // =========================================================================
    // RISK INTELLIGENCE — Predicción Batch Completa para "Inteligencia de Riesgo"
    // =========================================================================

    /**
     * Predice la fuga para TODOS los clientes de la BD usando el endpoint
     * batch del modelo Python y persiste los resultados como lote activo.
     *
     * Flujo:
     * 1. Cargar todos los clientes y sus cuentas en memoria.
     * 2. Enviar en chunks de {@code CHUNK_SIZE} al endpoint /churn/predict-batch.
     * 3. Por cada resultado: guardar ChurnPrediction + ChurnSampleEntry en BD.
     * 4. Marcar el nuevo ChurnSampleBatch como activo y desactivar el anterior.
     *
     * @param targetSize  Ignorado (se predicen todos los clientes). Mantenido
     *                    por compatibilidad con llamadas existentes.
     * @param triggeredBy 'scheduler' | 'manual'
     */
    public void buildRiskSample(int targetSize, String triggeredBy) {
        System.out.println("[BatchPredict] Iniciando prediccion batch completa. trigger=" + triggeredBy);

        // 1. Cargar todos los clientes
        List<Customer> allCustomers = customerRepository.findAll();
        long totalInDB = allCustomers.size();
        System.out.println("[BatchPredict] Total clientes en BD: " + totalInDB);

        if (allCustomers.isEmpty()) {
            System.err.println("[BatchPredict] No hay clientes en BD. Abortando.");
            return;
        }

        List<Long> allIds = allCustomers.stream()
                .map(Customer::getIdCustomer)
                .collect(Collectors.toList());

        // 2. Cargar cuentas
        java.util.Map<Long, AccountDetails> accountMap = new java.util.HashMap<>();
        accountDetailsRepository.findByCustomerIds(allIds)
                .forEach(a -> accountMap.putIfAbsent(a.getCustomer().getIdCustomer(), a));

        Map<Long, Customer> customerLookup = allCustomers.stream()
                .collect(Collectors.toMap(Customer::getIdCustomer, c -> c));

        // 3. Predecir TODOS los clientes en chunks de 1000 via /predict-batch
        final int CHUNK_SIZE = 1000;
        java.util.Map<Long, ChurnPredictions> newPredictions = new java.util.LinkedHashMap<>();
        int totalChunks = (int) Math.ceil((double) allIds.size() / CHUNK_SIZE);

        for (int i = 0; i < allIds.size(); i += CHUNK_SIZE) {
            List<Long> chunk = allIds.subList(i, Math.min(i + CHUNK_SIZE, allIds.size()));
            int chunkNum = (i / CHUNK_SIZE) + 1;
            System.out.printf("[BatchPredict] Chunk %d/%d — clientes %d-%d%n",
                    chunkNum, totalChunks, i + 1, Math.min(i + CHUNK_SIZE, (int) totalInDB));
            try {
                List<ChurnBatchResultItemDTO> results = callPythonApiBatch(chunk, customerLookup, accountMap);
                LocalDateTime now = LocalDateTime.now();
                for (ChurnBatchResultItemDTO r : results) {
                    Customer c = customerLookup.get(r.getId());
                    AccountDetails acc = accountMap.get(r.getId());
                    if (c == null || acc == null)
                        continue;

                    ChurnPredictions pred = new ChurnPredictions();
                    pred.setCustomer(c);
                    pred.setPredictionDate(now);
                    pred.setChurnProbability(BigDecimal.valueOf(r.getChurnProbability()));
                    pred.setIsChurn(r.getIsChurn() != null && r.getIsChurn() == 1);
                    pred.setRiskLevel(r.getRiskLevel());
                    pred.setModelVersion(r.getModelVersion() != null ? r.getModelVersion() : "v1");
                    pred.setCustomerValue(acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO);
                    double conf = r.getPredictionConfidence() != null ? r.getPredictionConfidence() : 0.95;
                    pred.setPredictionConfidence(BigDecimal.valueOf(conf));

                    ChurnPredictions saved = churnPredictionsRepository.save(pred);
                    newPredictions.put(r.getId(), saved);
                }
            } catch (Exception ex) {
                System.err.println("[BatchPredict] Error en chunk " + chunkNum + ": " + ex.getMessage());
            }
        }

        System.out.println("[BatchPredict] Clientes predichos: " + newPredictions.size() + " / " + totalInDB);

        // 4. Desactivar lotes anteriores y crear el nuevo
        churnSampleBatchRepository.deactivateAll();

        ChurnSampleBatch batch = new ChurnSampleBatch();
        batch.setCreatedAt(LocalDateTime.now());
        batch.setSampleSize((int) totalInDB);
        batch.setTotalCustomers(totalInDB);
        batch.setIsActive(true);
        batch.setTriggeredBy(triggeredBy);
        churnSampleBatchRepository.save(batch);

        // 5. Guardar entradas del lote en bulk
        List<ChurnSampleEntry> entries = new ArrayList<>(newPredictions.keySet()).stream()
                .map(id -> {
                    Customer c = customerLookup.get(id);
                    if (c == null)
                        return null;
                    ChurnSampleEntry entry = new ChurnSampleEntry();
                    entry.setBatch(batch);
                    entry.setCustomer(c);
                    return entry;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        churnSampleEntryRepository.saveAll(entries);

        System.out.println("[RiskSample] Lote guardado. ID=" + batch.getId());
    }

    /**
     * Devuelve la muestra activa con predicciones para "Inteligencia de Riesgo".
     * Si no hay lote activo, retorna hasSample=false.
     */
    @Transactional(readOnly = true)
    public RiskIntelligenceDTO getRiskIntelligenceData() {
        java.util.Optional<ChurnSampleBatch> activeBatch = churnSampleBatchRepository
                .findTopByIsActiveTrueOrderByCreatedAtDesc();

        if (activeBatch.isEmpty()) {
            return RiskIntelligenceDTO.builder()
                    .clients(new ArrayList<>())
                    .hasSample(false)
                    .totalCustomers(customerRepository.count())
                    .sampleSize(0)
                    .build();
        }

        ChurnSampleBatch batch = activeBatch.get();
        List<ChurnSampleEntry> entries = churnSampleEntryRepository.findByBatchWithCustomer(batch);

        List<Long> customerIds = entries.stream()
                .map(e -> e.getCustomer().getIdCustomer())
                .collect(Collectors.toList());

        // Carga batch de cuentas y predicciones
        java.util.Map<Long, AccountDetails> accountMap = new java.util.HashMap<>();
        for (AccountDetails ad : accountDetailsRepository.findByCustomerIds(customerIds)) {
            accountMap.putIfAbsent(ad.getCustomer().getIdCustomer(), ad);
        }
        java.util.Map<Long, ChurnPredictions> predMap = new java.util.HashMap<>();
        for (ChurnPredictions cp : churnPredictionsRepository.findLatestByCustomerIds(customerIds)) {
            predMap.putIfAbsent(cp.getCustomer().getIdCustomer(), cp);
        }

        // ── Muestra Estratificada para Matriz de Prioridad de Retención ──────
        // Solo se aplica cuando el batch supera matrixSampleSize.
        // Proporciones: Alto 50% · Medio 35% · Bajo 15%, ordenados por prob DESC.
        // Si un estrato tiene menos entradas que su cuota, el sobrante se
        // redistribuye al estrato de mayor prioridad siguiente.
        if (entries.size() > matrixSampleSize) {
            List<ChurnSampleEntry> alto = new ArrayList<>();
            List<ChurnSampleEntry> medio = new ArrayList<>();
            List<ChurnSampleEntry> bajo = new ArrayList<>();

            for (ChurnSampleEntry e : entries) {
                ChurnPredictions pred = predMap.get(e.getCustomer().getIdCustomer());
                String risk = (pred != null && pred.getRiskLevel() != null)
                        ? pred.getRiskLevel()
                        : "Bajo";
                if ("Alto".equals(risk))
                    alto.add(e);
                else if ("Medio".equals(risk))
                    medio.add(e);
                else
                    bajo.add(e);
            }

            java.util.Comparator<ChurnSampleEntry> byProbDesc = (a, b) -> {
                BigDecimal pa = Optional.ofNullable(predMap.get(a.getCustomer().getIdCustomer()))
                        .map(ChurnPredictions::getChurnProbability).orElse(BigDecimal.ZERO);
                BigDecimal pb = Optional.ofNullable(predMap.get(b.getCustomer().getIdCustomer()))
                        .map(ChurnPredictions::getChurnProbability).orElse(BigDecimal.ZERO);
                return pb.compareTo(pa);
            };
            alto.sort(byProbDesc);
            medio.sort(byProbDesc);
            bajo.sort(byProbDesc);

            int quotaAlto = (int) (matrixSampleSize * 0.50);
            int quotaMedio = (int) (matrixSampleSize * 0.35);
            int quotaBajo = matrixSampleSize - quotaAlto - quotaMedio;

            int takenAlto = Math.min(alto.size(), quotaAlto);
            int surplus1 = quotaAlto - takenAlto;
            int takenMedio = Math.min(medio.size(), quotaMedio + surplus1);
            int surplus2 = (quotaMedio + surplus1) - takenMedio;
            int takenBajo = Math.min(bajo.size(), quotaBajo + surplus2);

            List<ChurnSampleEntry> sampled = new ArrayList<>(takenAlto + takenMedio + takenBajo);
            sampled.addAll(alto.subList(0, takenAlto));
            sampled.addAll(medio.subList(0, takenMedio));
            sampled.addAll(bajo.subList(0, takenBajo));
            entries = sampled;

            System.out.printf(
                    "[RiskIntel] Muestra estratificada aplicada — Alto=%d Medio=%d Bajo=%d → total=%d (de %d en batch)%n",
                    takenAlto, takenMedio, takenBajo, entries.size(), batch.getSampleSize());
        }
        // ─────────────────────────────────────────────────────────────────────

        List<RiskClientDTO> clients = new ArrayList<>();
        for (ChurnSampleEntry entry : entries) {
            Customer c = entry.getCustomer();
            clients.add(buildRiskClientDTO(c,
                    accountMap.get(c.getIdCustomer()),
                    predMap.get(c.getIdCustomer())));
        }

        return RiskIntelligenceDTO.builder()
                .clients(clients)
                .lastUpdated(batch.getCreatedAt())
                .triggeredBy(batch.getTriggeredBy())
                .sampleSize(entries.size())
                .totalCustomers(batch.getTotalCustomers())
                .hasSample(true)
                .build();
    }

    /**
     * Refresco manual: construye un nuevo lote y devuelve estado.
     */
    @Transactional
    public java.util.Map<String, Object> refreshRiskSample(int targetSize) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            buildRiskSample(targetSize, "manual");
            result.put("status", "ok");
            result.put("message", "Muestra actualizada correctamente.");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Error al actualizar la muestra: " + e.getMessage());
        }
        return result;
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private RiskClientDTO buildRiskClientDTO(Customer c, AccountDetails acc, ChurnPredictions pred) {
        Integer risk = 50;
        boolean analyzed = false;
        if (pred != null && pred.getChurnProbability() != null) {
            risk = pred.getChurnProbability().multiply(BigDecimal.valueOf(100)).intValue();
            analyzed = true;
        }

        String country = (c.getCountry() != null && c.getCountry().getCountryDescription() != null)
                ? c.getCountry().getCountryDescription()
                : "Desconocido";

        String name = ((c.getFirstName() != null ? c.getFirstName() : "")
                + " " + (c.getSurname() != null ? c.getSurname() : "")).trim();

        double balance = (acc != null && acc.getBalance() != null) ? acc.getBalance().doubleValue() : 0.0;
        int products = (acc != null && acc.getNumOfProducts() != null) ? acc.getNumOfProducts() : 1;
        int score = (acc != null && acc.getCreditScore() != null) ? acc.getCreditScore() : 0;

        return RiskClientDTO.builder()
                .id(c.getIdCustomer())
                .name(name)
                .age(c.getAge() != null ? c.getAge() : 0)
                .balance(balance)
                .country(country)
                .risk(risk)
                .analyzed(analyzed)
                .products(products)
                .score(score)
                .email(c.getEmail())
                .build();
    }

    // ============================================================
    // LIVE MODEL STATUS (equivalente a ATM /v1/withdrawal/info y /update)
    // ============================================================

    /**
     * Returns live model status from the Python CHURN API.
     * Proxies GET /churn/model/info
     */
    public Map<String, Object> getModelInfo() {
        try {
            String url = churnApiBaseUrl + "/churn/model/info";
            System.out.println("---> Calling Python Model Info API: GET " + url);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return java.util.Map.of(
                        "status", "error",
                        "message", "La API de Python no devolvió respuesta.");
            }
            return response;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("---> Python API not reachable: " + e.getMessage());
            return java.util.Map.of(
                    "status", "error",
                    "message", "No se pudo conectar con la API de Python.");
        } catch (Exception e) {
            System.err.println("---> Model info error: " + e.getMessage());
            return java.util.Map.of(
                    "status", "error",
                    "message", "Error consultando información del modelo: " + e.getMessage());
        }
    }

    /**
     * Triggers a hot-reload of the CHURN model from DagsHub.
     * Proxies POST /churn/model/reload
     */
    public Map<String, Object> reloadModel() {
        try {
            String url = churnApiBaseUrl + "/churn/model/reload";
            System.out.println("---> Calling Python Model Reload API: POST " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null) {
                return java.util.Map.of(
                        "status", "error",
                        "message", "La API de Python no devolvió respuesta.");
            }
            return response;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("---> Python API not reachable: " + e.getMessage());
            return java.util.Map.of(
                    "status", "error",
                    "message", "No se pudo conectar con la API de Python.");
        } catch (Exception e) {
            System.err.println("---> Model reload error: " + e.getMessage());
            return java.util.Map.of(
                    "status", "error",
                    "message", "Error iniciando recarga del modelo: " + e.getMessage());
        }
    }

}
