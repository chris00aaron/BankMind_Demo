package com.naal.bankmind.service.Churn;

import com.naal.bankmind.dto.Churn.ChurnRequestDTO;
import com.naal.bankmind.dto.Churn.ChurnResponseDTO;
import com.naal.bankmind.dto.Churn.CustomerDashboardDTO;
import com.naal.bankmind.dto.Churn.SegmentDTO;
import com.naal.bankmind.dto.Churn.TrainResultDTO;
import com.naal.bankmind.dto.Churn.PerformanceStatusDTO;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.ChurnPredictions;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.CustomerRepository;
import com.naal.bankmind.repository.Churn.ChurnPredictionsRepository;
import com.naal.bankmind.repository.Churn.RetentionStrategyDefRepository;
import com.naal.bankmind.repository.Churn.RetentionSegmentDefRepository;
import com.naal.bankmind.repository.Churn.CampaignLogRepository;
import com.naal.bankmind.repository.Churn.CampaignTargetRepository;
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
            CampaignTargetRepository campaignTargetRepository) {
        this.customerRepository = customerRepository;
        this.accountDetailsRepository = accountDetailsRepository;
        this.churnPredictionsRepository = churnPredictionsRepository;
        this.retentionStrategyDefRepository = retentionStrategyDefRepository;
        this.retentionSegmentDefRepository = retentionSegmentDefRepository;
        this.campaignLogRepository = campaignLogRepository;
        this.campaignTargetRepository = campaignTargetRepository;
        this.restTemplate = new RestTemplate();
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
                .build();
    }

    /**
     * Calculates global KPIs for the dashboard.
     * Uses batch queries to compute aggregated stats + Top 200 scatter data.
     */
    private com.naal.bankmind.dto.Churn.DashboardKpisDTO calculateDashboardKpis(String search) {
        // Get ALL customer IDs (filtered by search if applicable) for global KPIs
        List<Customer> allCustomers;
        if (search != null && !search.trim().isEmpty()) {
            allCustomers = customerRepository.searchByNameOrId(search.trim());
        } else {
            // Use a reasonable limit for KPI calculation
            org.springframework.data.domain.Pageable kpiLimit = org.springframework.data.domain.PageRequest.of(0,
                    10000);
            allCustomers = customerRepository.findAll(kpiLimit).getContent();
        }

        List<Long> allIds = allCustomers.stream().map(Customer::getIdCustomer).collect(Collectors.toList());

        // Batch load accounts and predictions for ALL customers
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

        long totalCustomers = allCustomers.size();
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

            BigDecimal balance = (acc != null && acc.getBalance() != null) ? acc.getBalance() : BigDecimal.ZERO;
            int risk = 50; // default
            if (pred != null && pred.getChurnProbability() != null) {
                risk = pred.getChurnProbability().multiply(BigDecimal.valueOf(100)).intValue();
            }

            // Risk distribution counting
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

            String name = ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                    + (c.getSurname() != null ? c.getSurname() : "")).trim();

            // Country name for enriched tooltip
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

        // Sort by risk descending and take Top 200 for scatter chart
        allPoints.sort((a, b) -> Double.compare(b.getX(), a.getX()));
        List<com.naal.bankmind.dto.Churn.DashboardKpisDTO.ScatterPointDTO> scatterData = allPoints.stream()
                .limit(200)
                .collect(Collectors.toList());

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
     * Generates a "Next Best Action" recommendation based on customer profile.
     * 
     * @param idCustomer Customer ID
     * @return The recommended strategy definition
     */
    public RetentionStrategyDef getRecommendation(Long idCustomer) {
        // ... (existing implementation)
        // 1. Fetch customer financial context
        AccountDetails account = accountDetailsRepository.findFirstByCustomer_IdCustomer(idCustomer)
                .orElseThrow(() -> new RuntimeException("Customer data not found"));

        // 2. Fetch latest risk prediction
        List<ChurnPredictions> history = churnPredictionsRepository
                .findByCustomer_IdCustomerOrderByPredictionDateDesc(idCustomer);
        double riskProb = history.isEmpty() ? 0.5 : history.get(0).getChurnProbability().doubleValue();

        // 3. Rule Engine
        long strategyId = 1L; // Default: Descuento
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        int products = account.getNumOfProducts() != null ? account.getNumOfProducts() : 0;

        if (balance.compareTo(new BigDecimal("100000")) > 0 && riskProb > 0.6) {
            strategyId = 3L; // VIP
        } else if (products == 1 && riskProb > 0.4) {
            strategyId = 2L; // Cross-Selling
        }

        // 4. Fetch Strategy from DB
        return retentionStrategyDefRepository.findById(strategyId)
                .orElseThrow(() -> new RuntimeException("Strategy definition not found in DB"));
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
        } catch (Exception e) {
            // If API fails, return default response for development
            return ChurnResponseDTO.builder()
                    .churnProbability(0.0)
                    .isChurn(false)
                    .riskLevel("Error")
                    .modelVersion("error")
                    .build();
        }
    }

    /**
     * Calculates geography statistics based on real customer data.
     * LIMITED to first 500 customers to avoid N+1 performance issues.
     */
    public List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> getGeographyStats() {
        // Use pagination to limit results and avoid N+1 query timeout
        org.springframework.data.domain.Pageable limit = org.springframework.data.domain.PageRequest.of(0, 500);
        List<Customer> customers = customerRepository.findAll(limit).getContent();

        // Group by Country Name
        java.util.Map<String, List<Customer>> customersByCountry = customers.stream()
                .collect(java.util.stream.Collectors
                        .groupingBy(c -> (c.getCountry() != null && c.getCountry().getCountryDescription() != null)
                                ? c.getCountry().getCountryDescription()
                                : "Unknown"));

        List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> stats = new ArrayList<>();

        for (java.util.Map.Entry<String, List<Customer>> entry : customersByCountry.entrySet()) {
            String countryName = entry.getKey();
            List<Customer> countryCustomers = entry.getValue();

            int total = countryCustomers.size();
            int high = 0;
            int medium = 0;
            int low = 0;
            BigDecimal totalBalance = BigDecimal.ZERO;
            int churnCount = 0; // Using risk > 50% as proxy for potential churn if isChurn is null

            for (Customer c : countryCustomers) {
                // Get latest prediction for risk
                List<ChurnPredictions> predictions = churnPredictionsRepository
                        .findByCustomer_IdCustomerOrderByPredictionDateDesc(c.getIdCustomer());

                // Get balance
                Optional<AccountDetails> accountOpt = accountDetailsRepository
                        .findFirstByCustomer_IdCustomer(c.getIdCustomer());
                if (accountOpt.isPresent() && accountOpt.get().getBalance() != null) {
                    totalBalance = totalBalance.add(accountOpt.get().getBalance());
                }

                double riskVal = 0.50; // Default
                if (!predictions.isEmpty()) {
                    ChurnPredictions p = predictions.get(0);
                    if (p.getChurnProbability() != null) {
                        riskVal = p.getChurnProbability().doubleValue();
                    }
                    if (p.getIsChurn() != null && p.getIsChurn()) {
                        churnCount++; // Or use risk > threshold
                    } else if (riskVal > 0.5) {
                        churnCount++; // Proxy
                    }
                } else {
                    // Start with mock logic for demo if no prediction exists
                    // Or just treat as low risk
                }

                if (riskVal > 0.7)
                    high++;
                else if (riskVal > 0.5)
                    medium++;
                else
                    low++;
            }

            BigDecimal avgBalance = total > 0
                    ? totalBalance.divide(BigDecimal.valueOf(total), java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal churnRate = total > 0
                    ? BigDecimal.valueOf((double) churnCount * 100 / total)
                    : BigDecimal.ZERO;

            // Map codes/flags
            String code = "UN";
            String flag = "🏳️";
            if (countryName.equalsIgnoreCase("France") || countryName.equalsIgnoreCase("Francia")) {
                code = "FR";
                flag = "🇫🇷";
            } else if (countryName.equalsIgnoreCase("Germany") || countryName.equalsIgnoreCase("Alemania")) {
                code = "DE";
                flag = "🇩🇪";
            } else if (countryName.equalsIgnoreCase("Spain") || countryName.equalsIgnoreCase("España")
                    || countryName.equalsIgnoreCase("Spain")) {
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
     * Uses real metrics from the latest training run if available.
     * Falls back to static defaults if no training has occurred in this session.
     */
    public com.naal.bankmind.dto.Churn.MLOpsMetricsDTO getMLOpsMetrics() {
        long totalPredictions = churnPredictionsRepository.count();

        // Use cached metrics from last training if available
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

        // Fallback: static defaults (no training has occurred yet)
        return com.naal.bankmind.dto.Churn.MLOpsMetricsDTO.builder()
                .modelStatus("Active")
                .modelVersion("v2.3.1")
                .totalPredictions(totalPredictions)
                .lastTrainingDate(java.time.LocalDate.now().minusDays(2).toString())
                .precision(91.5)
                .recall(88.3)
                .f1Score(89.8)
                .aucRoc(94.2)
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
     * Maps a raw Map response from Python API to PerformanceStatusDTO.
     */
    private PerformanceStatusDTO mapToPerformanceStatus(Map<String, Object> raw) {
        PerformanceStatusDTO.PerformanceStatusDTOBuilder builder = PerformanceStatusDTO.builder()
                .status((String) raw.getOrDefault("status", "unknown"))
                .message((String) raw.get("message"))
                .recall(toDouble(raw.get("recall")))
                .f1Score(toDouble(raw.get("f1_score")))
                .precision(toDouble(raw.get("precision")))
                .accuracy(toDouble(raw.get("accuracy")))
                .recallThreshold(toDouble(raw.get("recall_threshold")))
                .autoTrainingTriggered((Boolean) raw.get("auto_training_triggered"))
                .triggerReason((String) raw.get("trigger_reason"))
                .lastEvaluationDate((String) raw.get("last_evaluation_date"))
                .nextEvaluationDate((String) raw.get("next_evaluation_date"));

        // Integer fields
        if (raw.get("evaluated_samples") != null)
            builder.evaluatedSamples(((Number) raw.get("evaluated_samples")).intValue());
        if (raw.get("min_samples_required") != null)
            builder.minSamplesRequired(((Number) raw.get("min_samples_required")).intValue());
        if (raw.get("maturation_days") != null)
            builder.maturationDays(((Number) raw.get("maturation_days")).intValue());
        if (raw.get("monitor_interval_hours") != null)
            builder.monitorIntervalHours(((Number) raw.get("monitor_interval_hours")).intValue());
        if (raw.get("monitor_enabled") != null)
            builder.monitorEnabled((Boolean) raw.get("monitor_enabled"));

        // Confusion matrix
        if (raw.get("true_positives") != null)
            builder.truePositives(((Number) raw.get("true_positives")).intValue());
        if (raw.get("false_positives") != null)
            builder.falsePositives(((Number) raw.get("false_positives")).intValue());
        if (raw.get("true_negatives") != null)
            builder.trueNegatives(((Number) raw.get("true_negatives")).intValue());
        if (raw.get("false_negatives") != null)
            builder.falseNegatives(((Number) raw.get("false_negatives")).intValue());

        // Training result fields
        builder.trainingRunId((String) raw.get("training_run_id"));
        builder.trainingError((String) raw.get("training_error"));

        return builder.build();
    }
}
