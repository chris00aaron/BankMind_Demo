package com.naal.bankmind.service.Default;

import com.naal.bankmind.client.Default.MorosidadFeignClient;
import com.naal.bankmind.dto.Default.Request.MorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.PredecirMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.BatchMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchMorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchItemResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchAccountPredictionDTO;
import com.naal.bankmind.dto.Default.Response.BatchPredictionWrapperDTO;
import com.naal.bankmind.dto.Default.Response.RiskFactorDTO;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.MonthlyHistory;
import com.naal.bankmind.entity.Default.DefaultPolicies;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.DefaultPoliciesRepository;
import com.naal.bankmind.repository.Default.DefaultPredictionRepository;
import com.naal.bankmind.repository.Default.MonthlyHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.naal.bankmind.dto.Default.Response.ClientePredictionDetailDTO;

/**
 * Servicio para predicción de morosidad.
 * Arma el JSON con datos del cliente + historial, llama a la API y guarda la
 * predicción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MorosidadService {

    private final MorosidadFeignClient morosidadClient;
    private final AccountDetailsRepository accountDetailsRepository;
    private final MonthlyHistoryRepository monthlyHistoryRepository;
    private final DefaultPredictionRepository defaultPredictionRepository;
    private final DefaultPoliciesRepository policiesRepository;
    private final DefaultPoliciesRepository defaultPoliciesRepository;

    /**
     * Realiza una predicción de morosidad para una cuenta.
     */
    @Transactional
    public MorosidadResponseDTO predecirMorosidad(PredecirMorosidadRequestDTO request) {
        log.info("Iniciando predicción de morosidad para recordId: {}", request.recordId());

        AccountDetails account = accountDetailsRepository.findById(request.recordId())
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + request.recordId()));

        Customer customer = account.getCustomer();
        log.info("Cliente encontrado: {} {}", customer.getFirstName(), customer.getSurname());

        List<MonthlyHistory> historial = monthlyHistoryRepository
                .findTop6ByRecordIdOrderByMonthlyPeriodDesc(request.recordId());

        if (historial.size() < 6) {
            throw new RuntimeException("Se requieren al menos 6 meses de historial. Encontrados: " + historial.size());
        }

        MorosidadRequestDTO apiRequest = armarRequestAPI(account, customer, historial);

        MorosidadResponseDTO response;
        try {
            response = morosidadClient.predict(apiRequest);
            log.info("Predicción recibida - Default: {}, Probabilidad: {}, Factor: {}",
                    response.isDefault(), response.probabilidadDefault(), response.mainRiskFactor());
        } catch (Exception e) {
            log.error("Error al obtener predicción de morosidad: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio de predicción de morosidad", e);
        }

        guardarPrediccion(historial.get(0), response);
        return response;
    }

    /**
     * Realiza predicciones batch para múltiples cuentas.
     * OPTIMIZADO: Solo 2 queries en lugar de N+1.
     */
    @Transactional
    public BatchPredictionWrapperDTO predecirBatch(List<Long> recordIds, boolean includeShap) {
        log.info("Iniciando predicción batch para {} cuentas", recordIds.size());

        if (recordIds.isEmpty()) {
            return new BatchPredictionWrapperDTO(new ArrayList<>(), 50.0, null);
        }

        // QUERY 1: Cargar todas las cuentas con sus clientes en una sola query
        List<AccountDetails> allAccounts = accountDetailsRepository.findAllWithCustomerByRecordIds(recordIds);
        Map<Long, AccountDetails> accountMap = allAccounts.stream()
                .collect(Collectors.toMap(AccountDetails::getRecordId, a -> a));

        log.info("Cargadas {} cuentas", allAccounts.size());

        // QUERY 2: Cargar todo el historial para todas las cuentas
        List<MonthlyHistory> allHistories = monthlyHistoryRepository.findAllByRecordIds(recordIds);

        // Agrupar historiales por recordId y limitar a 6 por cuenta
        Map<Long, List<MonthlyHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(h -> h.getAccountDetails().getRecordId()));

        // Limitar a 6 registros por cuenta (ya están ordenados por fecha DESC)
        historyMap.replaceAll((k, v) -> v.size() > 6 ? v.subList(0, 6) : v);

        log.info("Cargados historiales para {} cuentas", historyMap.size());

        // Preparar datos para la API
        List<AccountDetails> validAccounts = new ArrayList<>();
        List<List<MonthlyHistory>> validHistoriales = new ArrayList<>();
        List<MorosidadRequestDTO> apiRequests = new ArrayList<>();

        for (Long recordId : recordIds) {
            AccountDetails account = accountMap.get(recordId);
            if (account == null) {
                continue;
            }

            List<MonthlyHistory> historial = historyMap.get(recordId);
            if (historial == null || historial.size() < 6) {
                continue;
            }

            validAccounts.add(account);
            validHistoriales.add(historial);
            apiRequests.add(armarRequestAPI(account, account.getCustomer(), historial));
        }

        if (apiRequests.isEmpty()) {
            log.warn("No hay cuentas válidas para procesar");
            return new BatchPredictionWrapperDTO(new ArrayList<>(), 50.0, null);
        }

        log.info("Preparadas {} cuentas para predicción batch", apiRequests.size());

        BatchMorosidadRequestDTO batchRequest = new BatchMorosidadRequestDTO(apiRequests, includeShap);
        BatchMorosidadResponseDTO batchResponse;
        try {
            batchResponse = morosidadClient.predictBatch(batchRequest);
            log.info("Respuesta batch recibida: {} predicciones", batchResponse.getTotalProcessed());
        } catch (Exception e) {
            log.error("Error en predicción batch: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio de predicción batch", e);
        }

        DefaultPolicies activePolicy = policiesRepository.findByIsActiveTrue().orElse(null);
        BigDecimal lgd = activePolicy != null ? activePolicy.getFactorLgd() : BigDecimal.valueOf(0.45);
        Double umbralPolitica = activePolicy != null
                ? activePolicy.getThresholdApproval().doubleValue() * 100
                : 50.0;

        List<BatchAccountPredictionDTO> results = new ArrayList<>();
        List<DefaultPrediction> predictionsToSave = new ArrayList<>(); // Acumular para batch insert

        for (BatchItemResponseDTO item : batchResponse.getPredictions()) {
            int idx = item.getIndex();
            if (idx >= validAccounts.size())
                continue;

            AccountDetails account = validAccounts.get(idx);
            Customer customer = account.getCustomer();
            List<MonthlyHistory> historial = validHistoriales.get(idx);

            MorosidadResponseDTO singleResponse = new MorosidadResponseDTO(
                    item.isDefaultPayment(),
                    item.getProbabilidadDefault(),
                    item.getMainRiskFactor(),
                    null, // riskFactors - batch no calcula SHAP individual
                    batchResponse.getModelVersion());

            // Crear predicción usando política pre-cargada
            DefaultPrediction pred = crearPrediccionBatch(historial.get(0), singleResponse, activePolicy, lgd);
            predictionsToSave.add(pred);

            double probabilidadPago = (1.0 - item.getProbabilidadDefault()) * 100;
            String nivelRiesgo = calcularNivelRiesgo(probabilidadPago);

            String nombre = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                    (customer.getSurname() != null ? customer.getSurname() : "");
            String educacion = customer.getEducation() != null
                    ? mapEducation(customer.getEducation().getIdEducation())
                    : "Otro";
            String estadoCivil = customer.getMarriage() != null
                    ? mapMarriage(customer.getMarriage().getIdMarriage())
                    : "Otro";

            // Calcular pérdida estimada con política pre-cargada
            BigDecimal estimatedLoss = calcularPerdidaEstimadaConLgd(singleResponse, historial.get(0).getBillAmtX(),
                    lgd);

            results.add(new BatchAccountPredictionDTO(
                    customer.getIdCustomer(),
                    nombre.trim(),
                    customer.getAge(),
                    educacion,
                    estadoCivil,
                    account.getRecordId(),
                    account.getLimitBal(),
                    account.getBalance(),
                    probabilidadPago,
                    nivelRiesgo,
                    historial.get(0).getBillAmtX(),
                    estimatedLoss));
        }

        // Guardar todas las predicciones en una sola operación batch
        if (!predictionsToSave.isEmpty()) {
            defaultPredictionRepository.saveAll(predictionsToSave);
            log.info("Guardadas {} predicciones en batch", predictionsToSave.size());
        }

        log.info("Predicción batch completada: {} resultados", results.size());

        return new BatchPredictionWrapperDTO(results, umbralPolitica, batchResponse.getShapSummary());
    }

    /**
     * Realiza una predicción de morosidad y retorna todos los datos enriquecidos.
     */
    @Transactional
    public ClientePredictionDetailDTO predecirMorosidadCompleto(PredecirMorosidadRequestDTO request) {
        log.info("Iniciando predicción completa para recordId: {}", request.recordId());

        AccountDetails account = accountDetailsRepository.findById(request.recordId())
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + request.recordId()));
        Customer customer = account.getCustomer();
        List<MonthlyHistory> historial = monthlyHistoryRepository
                .findTop6ByRecordIdOrderByMonthlyPeriodDesc(request.recordId());

        if (historial.size() < 6) {
            throw new RuntimeException("Se requieren al menos 6 meses de historial. Encontrados: " + historial.size());
        }

        MorosidadRequestDTO apiRequest = armarRequestAPI(account, customer, historial);
        MorosidadResponseDTO response;
        try {
            response = morosidadClient.predict(apiRequest);
        } catch (Exception e) {
            log.error("Error al obtener predicción: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio de predicción", e);
        }

        guardarPrediccion(historial.get(0), response);

        int cuotasAtrasadas = calcularCuotasAtrasadas(historial);
        double historialPagos = calcularHistorialPagos(historial);
        int antiguedadMeses = calcularAntiguedad(customer);
        double probabilidadPago = (1.0 - response.probabilidadDefault()) * 100;
        String nivelRiesgo = calcularNivelRiesgo(probabilidadPago);
        BigDecimal estimatedLoss = calcularPerdidaEstimada(response, account.getLimitBal());

        // Nuevos cálculos: clasificación SBS, percentil y umbral
        DefaultPolicies policy = policiesRepository.findByIsActiveTrue().orElse(null);
        String clasificacionSBS = calcularClasificacionSBS(response.probabilidadDefault(), policy);
        Integer percentilRiesgo = calcularPercentilRiesgo(response.probabilidadDefault());
        Double umbralPolitica = policy != null ? policy.getThresholdApproval().doubleValue() * 100 : 50.0;

        String educacion = mapEducation(customer.getEducation() != null ? customer.getEducation().getIdEducation() : 4);
        String estadoCivil = mapMarriage(customer.getMarriage() != null ? customer.getMarriage().getIdMarriage() : 3);
        String nombre = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                (customer.getSurname() != null ? customer.getSurname() : "");
        String fechaRegistro = customer.getIdRegistrationDate() != null
                ? customer.getIdRegistrationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "";
        String ultimoPago = historial.get(0).getMonthlyPeriod() != null
                ? historial.get(0).getMonthlyPeriod().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "";

        return new ClientePredictionDetailDTO(
                customer.getIdCustomer(), nombre.trim(), customer.getAge(), educacion, estadoCivil, fechaRegistro,
                account.getRecordId(), account.getLimitBal(), account.getBalance(), account.getEstimatedSalary(),
                account.getTenure(), antiguedadMeses, cuotasAtrasadas, historialPagos, historial.get(0).getBillAmtX(),
                ultimoPago, response.isDefault(), probabilidadPago, nivelRiesgo,
                response.mainRiskFactor(), response.riskFactors(), response.modelVersion(), estimatedLoss,
                clasificacionSBS, percentilRiesgo, umbralPolitica);
    }

    private int calcularCuotasAtrasadas(List<MonthlyHistory> historial) {
        return (int) historial.stream().filter(h -> h.getPayX() != null && h.getPayX() > 0).count();
    }

    private double calcularHistorialPagos(List<MonthlyHistory> historial) {
        long pagosATiempo = historial.stream().filter(h -> h.getPayX() != null && h.getPayX() <= 0).count();
        return (pagosATiempo * 100.0) / historial.size();
    }

    private int calcularAntiguedad(Customer customer) {
        if (customer.getIdRegistrationDate() == null)
            return 0;
        return (int) ChronoUnit.MONTHS.between(customer.getIdRegistrationDate().toLocalDate(), LocalDate.now());
    }

    private String calcularNivelRiesgo(double probabilidadPago) {
        if (probabilidadPago >= 75)
            return "Bajo";
        if (probabilidadPago >= 50)
            return "Medio";
        if (probabilidadPago >= 25)
            return "Alto";
        return "Crítico";
    }

    /**
     * Calcula la clasificación SBS según la matriz de la política activa.
     * 
     * @param probabilidadDefault Probabilidad de default (0.0 a 1.0)
     * @param policy              Política activa (puede ser null)
     * @return Categoría SBS: Normal, CPP, Deficiente, Dudoso, Pérdida
     */
    private String calcularClasificacionSBS(double probabilidadDefault, DefaultPolicies policy) {
        if (policy == null || policy.getSbsClassificationMatrix() == null) {
            // Default sin política: usar umbrales estándar
            if (probabilidadDefault <= 0.05)
                return "Normal";
            if (probabilidadDefault <= 0.25)
                return "CPP";
            if (probabilidadDefault <= 0.60)
                return "Deficiente";
            if (probabilidadDefault <= 0.90)
                return "Dudoso";
            return "Pérdida";
        }

        // Buscar en la matriz de la política
        for (var rule : policy.getSbsClassificationMatrix()) {
            if (probabilidadDefault >= rule.getMin() && probabilidadDefault < rule.getMax()) {
                return rule.getCategoria();
            }
        }
        return "Pérdida"; // Default si no encaja en ningún rango
    }

    /**
     * Calcula el percentil de riesgo: qué porcentaje de cuentas tienen MENOR
     * riesgo.
     * 
     * @param probabilidadDefault Probabilidad de default del cliente (0.0 a 1.0)
     * @return Percentil 0-100 (100 = más riesgoso que el 100% de la cartera)
     */
    private Integer calcularPercentilRiesgo(double probabilidadDefault) {
        // Contar predicciones con menor probabilidad de default
        long totalPredicciones = defaultPredictionRepository.count();
        if (totalPredicciones == 0)
            return 50;

        long menorRiesgo = defaultPredictionRepository.countByDefaultProbabilityLessThan(
                BigDecimal.valueOf(probabilidadDefault));

        return (int) Math.round((menorRiesgo * 100.0) / totalPredicciones);
    }

    /**
     * Calcula la pérdida estimada usando la fórmula de Basilea:
     * EL = EAD × PD × LGD
     * Donde EAD = billAmtX (monto adeudado actual)
     * Busca la política activa (para requests individuales).
     */
    private BigDecimal calcularPerdidaEstimada(MorosidadResponseDTO response, BigDecimal billAmtX) {
        BigDecimal lgd = policiesRepository.findByIsActiveTrue()
                .map(DefaultPolicies::getFactorLgd)
                .orElse(BigDecimal.valueOf(0.45));
        return calcularPerdidaEstimadaConLgd(response, billAmtX, lgd);
    }

    /**
     * Calcula EL con LGD pre-cargado (para batch).
     * OPTIMIZADO: evita query repetida a default_policies.
     */
    private BigDecimal calcularPerdidaEstimadaConLgd(MorosidadResponseDTO response, BigDecimal billAmtX,
            BigDecimal lgd) {
        if (billAmtX == null || billAmtX.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // EL = EAD × PD × LGD
        return billAmtX
                .multiply(BigDecimal.valueOf(response.probabilidadDefault()))
                .multiply(lgd)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String mapEducation(Integer idEducation) {
        return switch (idEducation) {
            case 1 -> "Postgrado";
            case 2 -> "Universitaria";
            case 3 -> "Secundaria";
            case 4 -> "Primaria";
            default -> "Otro";
        };
    }

    private String mapMarriage(Integer idMarriage) {
        return switch (idMarriage) {
            case 1 -> "Casado";
            case 2 -> "Soltero";
            case 3 -> "Divorciado";
            default -> "Otro";
        };
    }

    private MorosidadRequestDTO armarRequestAPI(AccountDetails account, Customer customer,
            List<MonthlyHistory> historial) {
        BigDecimal limitBal = account.getLimitBal();
        BigDecimal billAmt1 = historial.get(0).getBillAmtX();
        Double utilizationRate = 0.0;

        if (limitBal != null && limitBal.compareTo(BigDecimal.ZERO) > 0 && billAmt1 != null) {
            utilizationRate = billAmt1.divide(limitBal, 4, RoundingMode.HALF_UP).doubleValue();
        }

        return new MorosidadRequestDTO(
                limitBal != null ? limitBal.doubleValue() : 0.0,
                customer.getGender() != null ? customer.getGender().getIdGender().intValue() : 1,
                customer.getEducation() != null ? customer.getEducation().getIdEducation().intValue() : 4,
                customer.getMarriage() != null ? customer.getMarriage().getIdMarriage().intValue() : 3,
                customer.getAge() != null ? customer.getAge() : 30,
                historial.get(0).getPayX() != null ? historial.get(0).getPayX() : 0,
                historial.get(1).getPayX() != null ? historial.get(1).getPayX() : 0,
                historial.get(2).getPayX() != null ? historial.get(2).getPayX() : 0,
                historial.get(3).getPayX() != null ? historial.get(3).getPayX() : 0,
                historial.get(4).getPayX() != null ? historial.get(4).getPayX() : 0,
                historial.get(5).getPayX() != null ? historial.get(5).getPayX() : 0,
                historial.get(0).getBillAmtX() != null ? historial.get(0).getBillAmtX().doubleValue() : 0.0,
                historial.get(1).getBillAmtX() != null ? historial.get(1).getBillAmtX().doubleValue() : 0.0,
                historial.get(2).getBillAmtX() != null ? historial.get(2).getBillAmtX().doubleValue() : 0.0,
                historial.get(3).getBillAmtX() != null ? historial.get(3).getBillAmtX().doubleValue() : 0.0,
                historial.get(4).getBillAmtX() != null ? historial.get(4).getBillAmtX().doubleValue() : 0.0,
                historial.get(5).getBillAmtX() != null ? historial.get(5).getBillAmtX().doubleValue() : 0.0,
                historial.get(0).getPayAmtX() != null ? historial.get(0).getPayAmtX().doubleValue() : 0.0,
                historial.get(1).getPayAmtX() != null ? historial.get(1).getPayAmtX().doubleValue() : 0.0,
                historial.get(2).getPayAmtX() != null ? historial.get(2).getPayAmtX().doubleValue() : 0.0,
                historial.get(3).getPayAmtX() != null ? historial.get(3).getPayAmtX().doubleValue() : 0.0,
                historial.get(4).getPayAmtX() != null ? historial.get(4).getPayAmtX().doubleValue() : 0.0,
                historial.get(5).getPayAmtX() != null ? historial.get(5).getPayAmtX().doubleValue() : 0.0,
                utilizationRate);
    }

    /**
     * Crea una predicción sin guardarla (para requests individuales).
     * Busca la política activa una vez.
     */
    private DefaultPrediction crearPrediccion(MonthlyHistory ultimoMes, MorosidadResponseDTO response) {
        DefaultPolicies policy = policiesRepository.findByIsActiveTrue().orElse(null);
        BigDecimal lgd = policy != null ? policy.getFactorLgd() : BigDecimal.valueOf(0.45);
        return crearPrediccionBatch(ultimoMes, response, policy, lgd);
    }

    /**
     * Crea una predicción sin guardarla, usando política pre-cargada.
     * OPTIMIZADO para batch: evita N queries a default_policies.
     */
    private DefaultPrediction crearPrediccionBatch(MonthlyHistory ultimoMes, MorosidadResponseDTO response,
            DefaultPolicies policy, BigDecimal lgd) {
        DefaultPrediction prediction = new DefaultPrediction();
        prediction.setMonthlyHistory(ultimoMes);
        prediction.setDatePrediction(LocalDateTime.now());
        prediction.setDefaultPaymentNextMonth(response.isDefault());
        prediction.setDefaultProbability(BigDecimal.valueOf(response.probabilidadDefault()));
        prediction.setMainRiskFactor(response.mainRiskFactor());

        // Calcular pérdida usando billAmtX como EAD con LGD pre-cargado
        BigDecimal billAmtX = ultimoMes.getBillAmtX();
        prediction.setEstimatedLoss(calcularPerdidaEstimadaConLgd(response, billAmtX, lgd));

        // Asociar la política pre-cargada
        if (policy != null) {
            prediction.setIdPolicy(policy);
        }

        return prediction;
    }

    /**
     * Guarda una predicción individual (para requests individuales).
     */
    private void guardarPrediccion(MonthlyHistory ultimoMes, MorosidadResponseDTO response) {
        DefaultPrediction prediction = crearPrediccion(ultimoMes, response);
        defaultPredictionRepository.save(prediction);
        log.info("Predicción guardada con ID: {}", prediction.getIdPrediction());
    }

    /**
     * Simula una predicción sin guardar en base de datos.
     * Incluye factores SHAP, pérdida estimada y clasificación SBS.
     */
    public com.naal.bankmind.dto.Default.Response.SimulationResponseDTO simulatePrediction(
            com.naal.bankmind.dto.Default.Request.SimulationRequestDTO request) {

        log.info("Simulando predicción de morosidad...");

        // Obtener política activa
        DefaultPolicies activePolicy = defaultPoliciesRepository.findByIsActiveTrue().orElse(null);

        // Construir request para API Python
        MorosidadRequestDTO apiRequest = new MorosidadRequestDTO(
                request.limitBal(),
                request.sex(),
                request.education(),
                request.marriage(),
                request.age(),
                request.pay0(), request.pay2(), request.pay3(), request.pay4(), request.pay5(), request.pay6(),
                request.billAmt1(), request.billAmt2(), request.billAmt3(), request.billAmt4(), request.billAmt5(),
                request.billAmt6(),
                request.payAmt1(), request.payAmt2(), request.payAmt3(), request.payAmt4(), request.payAmt5(),
                request.payAmt6(),
                request.utilizationRate());

        MorosidadResponseDTO response;
        try {
            response = morosidadClient.predict(apiRequest);
        } catch (Exception e) {
            log.error("Error en simulación: {}", e.getMessage());
            throw new RuntimeException("Error al simular predicción", e);
        }

        // Calcular EL: BILL_AMT1 * PD * LGD
        double lgd = activePolicy != null ? activePolicy.getFactorLgd().doubleValue() : 0.45;
        double ead = request.billAmt1(); // EAD = factura actual
        BigDecimal estimatedLoss = BigDecimal.valueOf(ead * response.probabilidadDefault() * lgd)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Obtener umbral de política
        Double umbralPolitica = activePolicy != null ? activePolicy.getThresholdApproval().doubleValue() : 0.30;

        // Calcular clasificación SBS
        String clasificacionSBS = calcularClasificacionSBS(response.probabilidadDefault(), activePolicy);

        log.info("Simulación completada: PD={}%, EL={}, clasificación={}",
                response.probabilidadDefault() * 100, estimatedLoss, clasificacionSBS);

        return new com.naal.bankmind.dto.Default.Response.SimulationResponseDTO(
                response.isDefault(),
                response.probabilidadDefault(),
                response.mainRiskFactor(),
                response.riskFactors(),
                estimatedLoss,
                umbralPolitica,
                clasificacionSBS,
                response.modelVersion());
    }
}
