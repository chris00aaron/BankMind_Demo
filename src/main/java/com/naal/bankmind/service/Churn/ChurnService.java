package com.naal.bankmind.service.Churn;

import com.naal.bankmind.dto.Churn.CampaignLogDTO;
import com.naal.bankmind.dto.Churn.ChurnRequestDTO;
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
import java.time.format.TextStyle;
import java.util.ArrayList;
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

    private final CustomerRepository customerRepository;
    private final AccountDetailsRepository accountDetailsRepository;
    private final ChurnPredictionsRepository churnPredictionsRepository;
    private final RetentionStrategyDefRepository retentionStrategyDefRepository;
    private final RetentionSegmentDefRepository retentionSegmentDefRepository;
    private final CampaignLogRepository campaignLogRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    private final ChurnTrainingHistoryRepository churnTrainingHistoryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${churn.api.base-url:http://localhost:8001}")
    private String churnApiBaseUrl;

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
        factory.setReadTimeout(120000); // 2 minutes to read (training can be slow)
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
     * @param riskLevel Optional risk level filter: "alto" (>70), "medio" (50-70),
     *                  "bajo" (<50)
     * @return CustomerPageDTO with paginated content and global KPIs
     */
    public com.naal.bankmind.dto.Churn.CustomerPageDTO getCustomersPaginated(int page, int size, String search,
            String country, String riskLevel) {
        System.out.println("Service: Fetching customers page=" + page + " size=" + size + " search='" + search
                + "' country='" + country + "' riskLevel='" + riskLevel + "'");

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // 1. Paginated query: get only the customers for this page
        org.springframework.data.domain.Page<Customer> customerPage;
        if (search != null && !search.trim().isEmpty()) {
            customerPage = customerRepository.searchByNameOrIdPageable(search.trim(), pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }

        List<Customer> customers = customerPage.getContent();
        System.out.println("Service: Page has " + customers.size() + " customers (total: "
                + customerPage.getTotalElements() + ")");

        // 2. Extract IDs for batch loading
        List<Long> customerIds = customers.stream()
                .map(Customer::getIdCustomer)
                .collect(Collectors.toList());

        // 3. Batch load: accounts and predictions in 2 queries (not N*2)
        Map<Long, AccountDetails> accountMap = new java.util.HashMap<>();
        Map<Long, ChurnPredictions> predictionMap = new java.util.HashMap<>();

        if (!customerIds.isEmpty()) {
            // Load all accounts for this page's customers
            List<AccountDetails> accounts = accountDetailsRepository.findByCustomerIds(customerIds);
            for (AccountDetails ad : accounts) {
                // Keep the first account per customer (same as original logic)
                accountMap.putIfAbsent(ad.getCustomer().getIdCustomer(), ad);
            }

            // Load latest prediction for each customer
            List<ChurnPredictions> predictions = churnPredictionsRepository.findLatestByCustomerIds(customerIds);
            for (ChurnPredictions cp : predictions) {
                predictionMap.putIfAbsent(cp.getCustomer().getIdCustomer(), cp);
            }
        }

        // 4. Build DTOs using the pre-loaded maps (no additional queries!)
        List<CustomerDashboardDTO> content = new ArrayList<>();
        for (Customer customer : customers) {
            try {
                CustomerDashboardDTO dto = buildCustomerDTO(customer, accountMap.get(customer.getIdCustomer()),
                        predictionMap.get(customer.getIdCustomer()));
                // Apply server-side filters (country and riskLevel)
                if (country != null && !country.trim().isEmpty()
                        && !dto.getCountry().equalsIgnoreCase(country.trim())) {
                    continue; // skip customers not matching country filter
                }
                if (riskLevel != null && !riskLevel.trim().isEmpty()) {
                    boolean match = false;
                    switch (riskLevel.trim().toLowerCase()) {
                        case "alto":
                            match = dto.getRisk() > 70;
                            break;
                        case "medio":
                            match = dto.getRisk() >= 50 && dto.getRisk() <= 70;
                            break;
                        case "bajo":
                            match = dto.getRisk() < 50;
                            break;
                        default:
                            match = true;
                    }
                    if (!match)
                        continue;
                }
                content.add(dto);
            } catch (Exception e) {
                System.err.println("Error processing customer ID " + customer.getIdCustomer() + ": " + e.getMessage());
            }
        }

        // 5. Calculate global KPIs (uses ALL customers, not just the page)
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

        // Risk score: from latest prediction or default 50
        Integer riskScore = 50;
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
    private com.naal.bankmind.dto.Churn.DashboardKpisDTO calculateDashboardKpis(String search) {
        // ── Real total count from DB (no page cap) ──────────────────────────
        long totalCustomers;
        List<Customer> allCustomers;

        if (search != null && !search.trim().isEmpty()) {
            allCustomers = customerRepository.searchByNameOrId(search.trim());
            totalCustomers = allCustomers.size();
        } else {
            // Exact total from DB — not capped by any page size
            totalCustomers = customerRepository.count();
            // Load up to 2000 customers for risk/scatter calculation
            org.springframework.data.domain.Pageable kpiLimit = org.springframework.data.domain.PageRequest.of(0, 2000);
            allCustomers = customerRepository.findAll(kpiLimit).getContent();
        }

        List<Long> allIds = allCustomers.stream().map(Customer::getIdCustomer).collect(Collectors.toList());

        // Batch load accounts and predictions for the sample customers
        Map<Long, AccountDetails> allAccounts = new java.util.HashMap<>();
        Map<Long, ChurnPredictions> allPredictions = new java.util.HashMap<>();

        if (!allIds.isEmpty()) {
            for (AccountDetails ad : accountDetailsRepository.findByCustomerIds(allIds)) {
                allAccounts.putIfAbsent(ad.getCustomer().getIdCustomer(), ad);
            }
            for (ChurnPredictions cp : churnPredictionsRepository.findLatestByCustomerIds(allIds)) {
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

            // Risk distribution counting (todos los clientes, con o sin cuenta)
            if (risk > 70) {
                highRiskCount++;
            } else if (risk >= 50) {
                mediumRiskCount++;
            } else {
                lowRiskCount++;
            }

            if (risk >= 50) {
                customersAtRisk++;
                capitalAtRisk = capitalAtRisk
                        .add(balance.multiply(BigDecimal.valueOf(risk)).divide(BigDecimal.valueOf(100),
                                java.math.RoundingMode.HALF_UP));
            }

            // Solo agregar al scatter si el cliente tiene cuenta con balance > 0
            // o si tiene una predicción real — evita colapsar todos en (50, 0)
            if (!hasAccount && !hasPrediction)
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

        double retentionRate = totalCustomers > 0
                ? ((double) (totalCustomers - customersAtRisk) / totalCustomers) * 100.0
                : 0.0;

        // ── Stratified scatter sample: up to 150 per quadrant (max 600 total) ──────
        // Thresholds mirror the frontend constants (RISK_THRESHOLD=50,
        // BALANCE_THRESHOLD=100000)
        final int RISK_THRESHOLD = 50;
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
    public RetentionStrategyDef getRecommendation(Long idCustomer) {
        // 1. Fetch customer financial context
        AccountDetails account = accountDetailsRepository.findFirstByCustomer_IdCustomer(idCustomer)
                .orElseThrow(() -> new RuntimeException("Customer data not found"));

        // 2. Fetch latest risk prediction
        List<ChurnPredictions> history = churnPredictionsRepository
                .findByCustomer_IdCustomerOrderByPredictionDateDesc(idCustomer);
        double riskProb = history.isEmpty() ? 0.5 : history.get(0).getChurnProbability().doubleValue();

        // 3. Realistic Banking Rule Engine
        long strategyId = 1L; // Default: Exoneración de Comisiones (Segmento Estándar)
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        int products = account.getNumOfProducts() != null ? account.getNumOfProducts() : 0;

        // PRIORIDAD 1: Valor del cliente (VIP)
        if (balance.compareTo(new BigDecimal("100000")) > 0) {
            // Los clientes de alto patrimonio buscan exclusividad y rendimiento.
            strategyId = 3L; // Upgrade Banca Premium / Gestor Personal
        }
        // PRIORIDAD 2: Vinculación (Cross-selling)
        else if (products == 1 && riskProb > 0.3) {
            // Si solo tiene un producto, es fácil que se vaya. Necesitamos "amarrarlo" con
            // crédito.
            strategyId = 2L; // Tarjeta de Crédito Pre-aprobada (0 Mantenimiento)
        }
        // PRIORIDAD 3: Retención Estándar
        else {
            // El cliente común odia las comisiones. Eliminar la fricción es la mejor
            // retención.
            strategyId = 1L; // Exoneración de Comisiones de Mantenimiento
        }

        // 4. Fetch Strategy from DB
        return retentionStrategyDefRepository.findById(strategyId)
                .orElseThrow(
                        () -> new RuntimeException("Strategy definition not found in DB. Ensure IDs 1, 2, 3 exist."));
    }

    /**
     * Logs an interaction with the customer (e.g., Offer Sent).
     * 
     * @param idCustomer Customer ID
     * @param actionType The type of action (EMAIL, CALL, etc.)
     */
    public void logInteraction(Long idCustomer, String actionType) {
        // ... implementation
        CampaignLog campaign = campaignLogRepository.findById(1L).orElseGet(() -> {
            CampaignLog newCamp = new CampaignLog();
            newCamp.setIdCampaign(1L); // Force ID 1 if possible, or let auto-gen
            newCamp.setName("Interacciones Individuales");
            newCamp.setStatus("ACTIVE");
            newCamp.setStartDate(LocalDateTime.now());
            return campaignLogRepository.save(newCamp);
        });

        // 2. Create Target Entry
        CampaignTarget target = new CampaignTarget();
        CampaignTargetKey key = new CampaignTargetKey();
        key.setIdCampaign(campaign.getIdCampaign());
        key.setIdCustomer(idCustomer);

        target.setId(key);
        target.setStatus("CONTACTED_" + actionType);
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
                    .error((String) rawResponse.get("error"));

            // Parse train/test samples
            if (rawResponse.get("train_samples") != null) {
                builder.trainSamples(((Number) rawResponse.get("train_samples")).intValue());
            }
            if (rawResponse.get("test_samples") != null) {
                builder.testSamples(((Number) rawResponse.get("test_samples")).intValue());
            }

            // Parse nested metrics object
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

            TrainResultDTO result = builder.build();

            // Cache for getMLOpsMetrics
            if ("success".equals(result.getStatus())) {
                lastTrainResult = result;
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
     * Calculates geography statistics based on real customer data.
     * 
     * ENHANCEMENT: Before calculating, it selects a random 10% of customers 
     * who haven't been analyzed recently and triggers their prediction.
     * This ensures the "Risk Intelligence" view is always fresh and diverse.
     */
    /**
     * Calculates geography statistics based on real customer data.
     * Uses batch queries to eliminate N+1 performance issues.
     */
    public List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> getGeographyStats() {
        // Load up to 2000 customers for a representative strategic sample
        org.springframework.data.domain.Pageable limit = org.springframework.data.domain.PageRequest.of(0, 2000);
        List<Customer> customers = customerRepository.findAll(limit).getContent();

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
                if (acc == null && pred == null) continue;

                total++;
                if (acc != null && acc.getBalance() != null) {
                    totalBalance = totalBalance.add(acc.getBalance());
                }

                double riskVal = 0.50; // Default when no prediction exists
                if (pred != null) {
                    if (pred.getChurnProbability() != null) {
                        riskVal = pred.getChurnProbability().doubleValue();
                    }
                    if (Boolean.TRUE.equals(pred.getIsChurn()) || riskVal > 0.5) {
                        churnCount++;
                    }
                }

                if (riskVal > 0.7) high++;
                else if (riskVal > 0.5) medium++;
                else low++;
            }

            if (total == 0) continue;

            BigDecimal avgBalance = totalBalance.divide(BigDecimal.valueOf(total), java.math.RoundingMode.HALF_UP);
            BigDecimal churnRate = BigDecimal.valueOf((double) churnCount * 100 / total);

            // Map country codes and flags
            String code = "UN";
            String flag = "🏳️";
            String cLower = countryName.toLowerCase();
            if (cLower.contains("fran")) { code = "FR"; flag = "🇫🇷"; }
            else if (cLower.contains("alem") || cLower.contains("germ")) { code = "DE"; flag = "🇩🇪"; }
            else if (cLower.contains("espa") || cLower.contains("spai")) { code = "ES"; flag = "🇪🇸"; }

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
                    .lastTrainingDate(java.time.LocalDate.now().toString())
                    .precision(m.getPrecision() != null ? m.getPrecision() * 100 : 91.5)
                    .recall(m.getRecall() != null ? m.getRecall() * 100 : 88.3)
                    .f1Score(m.getF1Score() != null ? m.getF1Score() * 100 : 89.8)
                    .aucRoc(m.getAucRoc() != null ? m.getAucRoc() * 100 : 94.2)
                    .build();
        }

        // Priority 2: Read real metrics from churn_training_history table (M3 fix)
        Optional<ChurnTrainingHistory> latestTraining = churnTrainingHistoryRepository
                .findTopByOrderByTrainingDateDesc();
        if (latestTraining.isPresent()) {
            ChurnTrainingHistory h = latestTraining.get();
            return com.naal.bankmind.dto.Churn.MLOpsMetricsDTO.builder()
                    .modelStatus("Active")
                    .modelVersion(h.getModelVersion() != null ? h.getModelVersion() : "v-db")
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

        // Priority 3: Static defaults (only if no training has EVER occurred)
        return com.naal.bankmind.dto.Churn.MLOpsMetricsDTO.builder()
                .modelStatus("No Training Yet")
                .modelVersion("none")
                .totalPredictions(totalPredictions)
                .lastTrainingDate("N/A")
                .precision(0.0)
                .recall(0.0)
                .f1Score(0.0)
                .aucRoc(0.0)
                .build();
    }

    // ============================================================
    // CAMPAIGN MANAGEMENT (M2 — Real Persistence)
    // ============================================================

    /**
     * Gets all campaigns from the database, ordered by start date descending.
     */
    public List<CampaignLogDTO> getCampaigns() {
        List<CampaignLog> campaigns = campaignLogRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "startDate"));

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
                .convertedCount(c.getConvertedCount() != null ? c.getConvertedCount() : 0)
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
            for (Long customerId : req.getTargets()) {
                try {
                    CampaignTarget target = new CampaignTarget();
                    CampaignTargetKey key = new CampaignTargetKey();
                    key.setIdCampaign(campaign.getIdCampaign());
                    key.setIdCustomer(customerId);
                    target.setId(key);
                    target.setStatus("PENDING");
                    target.setContactDate(LocalDateTime.now());
                    campaignTargetRepository.save(target);
                } catch (Exception e) {
                    System.err.println("Error adding target customer " + customerId + ": " + e.getMessage());
                }
            }
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
        // 1. Fetch data samples (Up to 2000 for accuracy)
        org.springframework.data.domain.Pageable limit = org.springframework.data.domain.PageRequest.of(0, 2000);
        List<Customer> customers = customerRepository.findAll(limit).getContent();
        List<Long> ids = customers.stream().map(Customer::getIdCustomer).collect(Collectors.toList());

        Map<Long, AccountDetails> accounts = new java.util.HashMap<>();
        accountDetailsRepository.findByCustomerIds(ids).forEach(a -> accounts.put(a.getCustomer().getIdCustomer(), a));

        Map<Long, ChurnPredictions> predictions = new java.util.HashMap<>();
        churnPredictionsRepository.findLatestByCustomerIds(ids)
                .forEach(p -> predictions.put(p.getCustomer().getIdCustomer(), p));

        // 2. Aggregate Business KPIs
        BigDecimal totalErosionProyectada = BigDecimal.ZERO;
        BigDecimal totalCapitalVIPAtRisk = BigDecimal.ZERO;
        long customersSavedCount = 0;
        BigDecimal investmentCost = BigDecimal.ZERO;
        BigDecimal estimatedSavings = BigDecimal.ZERO;

        for (Customer c : customers) {
            AccountDetails acc = accounts.get(c.getIdCustomer());
            ChurnPredictions pred = predictions.get(c.getIdCustomer());

            if (acc == null || pred == null)
                continue;

            BigDecimal balance = acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO;
            double risk = pred.getChurnProbability() != null ? pred.getChurnProbability().doubleValue() : 0.5;

            // Erosión Proyectada (CEO focus)
            BigDecimal erosion = balance.multiply(BigDecimal.valueOf(risk));
            totalErosionProyectada = totalErosionProyectada.add(erosion);

            if (balance.compareTo(new BigDecimal("100000")) > 0 && risk > 0.5) {
                totalCapitalVIPAtRisk = totalCapitalVIPAtRisk.add(balance);
            }
        }

        // 3. Calculate ROI from Campaigns (M2 Data)
        List<CampaignLog> activeCampaigns = campaignLogRepository.findAll();
        for (CampaignLog camp : activeCampaigns) {
            BigDecimal cost = camp.getBudgetAllocated() != null ? camp.getBudgetAllocated() : BigDecimal.ZERO;
            investmentCost = investmentCost.add(cost);

            // Simulation of Savings: Real bank ROI usually ranges from 5x to 12x
            // We use the strategy impact factor as a multiplier for realism
            if (camp.getStrategy() != null) {
                double multiplier = camp.getStrategy().getImpactFactor() != null
                        ? camp.getStrategy().getImpactFactor().doubleValue() * 40 // Impact 0.25 -> 10x multiplier
                        : 10.0;
                estimatedSavings = estimatedSavings.add(cost.multiply(BigDecimal.valueOf(multiplier)));
            }
        }

        // Final ROI = (Savings - Cost) / Cost
        double roi = 0.0;
        if (investmentCost.compareTo(BigDecimal.ZERO) > 0) {
            roi = estimatedSavings.subtract(investmentCost)
                    .divide(investmentCost, 2, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 4. Final Payload for Executive View
        Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("capitalErosionProyectada", totalErosionProyectada.setScale(2, java.math.RoundingMode.HALF_UP));
        metrics.put("vipCapitalAtRisk", totalCapitalVIPAtRisk.setScale(2, java.math.RoundingMode.HALF_UP));
        metrics.put("retentionROI", roi);
        metrics.put("totalInvestment", investmentCost.setScale(2, java.math.RoundingMode.HALF_UP));
        metrics.put("estimatedSavings", estimatedSavings.setScale(2, java.math.RoundingMode.HALF_UP));
        metrics.put("efficiencyScore", roi > 7 ? "MÁXIMA" : roi > 4 ? "ALTA" : roi > 0 ? "OPTIMIZABLE" : "SIN DATOS");

        // Strategic Causes (Top-level insights)
        List<Map<String, Object>> strategicInsights = new ArrayList<>();
        strategicInsights.add(Map.of("cause", "Competencia de Tasas", "impact", "ALTO", "segment", "VIP"));
        strategicInsights.add(Map.of("cause", "Falta de Vinculación", "impact", "MEDIO", "segment", "Jóvenes"));
        strategicInsights.add(Map.of("cause", "Fricción por Comisiones", "impact", "ALTO", "segment", "Personal"));

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
                    ? h.getTrainingDate().toLocalDate().toString()
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
     * Returns the churn probability distribution grouped into 10 buckets (0-10%, 10-20%, ..., 90-100%).
     * Used by the "Distribución de Probabilidades" histogram.
     */
    public List<PredictionBucketDTO> getPredictionDistribution() {
        List<java.math.BigDecimal> probabilities = churnPredictionsRepository.findAllChurnProbabilities();

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
}
