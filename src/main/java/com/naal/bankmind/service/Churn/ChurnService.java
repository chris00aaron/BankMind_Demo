package com.naal.bankmind.service.Churn;

import com.naal.bankmind.dto.Churn.ChurnRequestDTO;
import com.naal.bankmind.dto.Churn.ChurnResponseDTO;
import com.naal.bankmind.dto.Churn.CustomerDashboardDTO;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.ChurnPredictions;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.CustomerRepository;
import com.naal.bankmind.repository.Churn.ChurnPredictionsRepository;
import com.naal.bankmind.repository.Churn.RetentionStrategyDefRepository;
import com.naal.bankmind.repository.Churn.CampaignLogRepository;
import com.naal.bankmind.repository.Churn.CampaignTargetRepository;
import com.naal.bankmind.entity.RetentionStrategyDef;
import com.naal.bankmind.entity.CampaignLog;
import com.naal.bankmind.entity.CampaignTarget;
import com.naal.bankmind.entity.CampaignTargetKey;
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

    private final CustomerRepository customerRepository;
    private final AccountDetailsRepository accountDetailsRepository;
    private final ChurnPredictionsRepository churnPredictionsRepository;
    private final RetentionStrategyDefRepository retentionStrategyDefRepository;
    private final CampaignLogRepository campaignLogRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    private final RestTemplate restTemplate;

    @Value("${churn.api.base-url:http://localhost:8000}")
    private String churnApiBaseUrl;

    public ChurnService(
            CustomerRepository customerRepository,
            AccountDetailsRepository accountDetailsRepository,
            ChurnPredictionsRepository churnPredictionsRepository,
            RetentionStrategyDefRepository retentionStrategyDefRepository,
            CampaignLogRepository campaignLogRepository,
            CampaignTargetRepository campaignTargetRepository) {
        this.customerRepository = customerRepository;
        this.accountDetailsRepository = accountDetailsRepository;
        this.churnPredictionsRepository = churnPredictionsRepository;
        this.retentionStrategyDefRepository = retentionStrategyDefRepository;
        this.campaignLogRepository = campaignLogRepository;
        this.campaignTargetRepository = campaignTargetRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Gets all customers for the dashboard display.
     * Includes their latest risk score from predictions.
     * 
     * @return List of CustomerDashboardDTO
     */
    public List<CustomerDashboardDTO> getAllCustomersForDashboard() {
        System.out.println("Service: Fetching all customers from DB...");
        List<Customer> customers = customerRepository.findAll();
        System.out.println("Service: Raw customers found: " + customers.size());
        
        List<CustomerDashboardDTO> result = new ArrayList<>();

        for (Customer customer : customers) {
            try {
                // Get first account for credit score and balance
                Optional<AccountDetails> accountOpt = accountDetailsRepository
                        .findFirstByCustomer_IdCustomer(customer.getIdCustomer());

                // Get latest prediction for risk score
                List<ChurnPredictions> predictions = churnPredictionsRepository
                        .findByCustomer_IdCustomerOrderByPredictionDateDesc(customer.getIdCustomer());

                Integer creditScore = 0;
                BigDecimal balance = BigDecimal.ZERO;

                if (accountOpt.isPresent()) {
                    AccountDetails account = accountOpt.get();
                    creditScore = account.getCreditScore() != null ? account.getCreditScore() : 0;
                    balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
                }

                // Risk score: from latest prediction or default 50
                Integer riskScore = 50;
                if (!predictions.isEmpty()) {
                    BigDecimal probability = predictions.get(0).getChurnProbability();
                    if (probability != null) {
                        riskScore = probability.multiply(BigDecimal.valueOf(100)).intValue();
                    }
                }

                // Get country name
                String country = "Desconocido";
                if (customer.getCountry() != null && customer.getCountry().getCountryDescription() != null) {
                    country = customer.getCountry().getCountryDescription();
                }

                result.add(CustomerDashboardDTO.builder()
                        .id(customer.getIdCustomer())
                        .score(creditScore)
                        .age(customer.getAge() != null ? customer.getAge() : 0)
                        .balance(balance)
                        .country(country)
                        .name((customer.getFirstName() != null ? customer.getFirstName() : "") + " "
                                + (customer.getSurname() != null ? customer.getSurname() : ""))
                        .risk(riskScore)
                        .build());
            } catch (Exception e) {
                System.err.println("Error processing customer ID " + customer.getIdCustomer() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Service: Returning " + result.size() + " processed DTOs.");
        return result;
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
        Double confidence = responseDTO.getPredictionConfidence() != null ? responseDTO.getPredictionConfidence() : 0.95;
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
        List<ChurnPredictions> history = churnPredictionsRepository.findByCustomer_IdCustomerOrderByPredictionDateDesc(idCustomer);
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
                System.out.println("--> Python Risk Factors: " + (response.getRiskFactors() != null ? response.getRiskFactors().size() : "NULL"));
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
     */
    public List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> getGeographyStats() {
        List<Customer> customers = customerRepository.findAll();

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
            } else if (countryName.equalsIgnoreCase("Spain") || countryName.equalsIgnoreCase("España")) {
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
     * Gets MLOps metrics (Simulated based on real counts).
     */
    public com.naal.bankmind.dto.Churn.MLOpsMetricsDTO getMLOpsMetrics() {
        long totalPredictions = churnPredictionsRepository.count();
        String lastDate = LocalDateTime.now().toString(); // Default

        // Static metrics for "High Fidelity" demo
        // In real productive app, these would come from Python API
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
}
