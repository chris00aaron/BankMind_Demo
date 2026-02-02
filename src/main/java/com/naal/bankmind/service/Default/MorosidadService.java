package com.naal.bankmind.service.Default;

import com.naal.bankmind.client.Default.MorosidadFeignClient;
import com.naal.bankmind.dto.Default.Request.MorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.PredecirMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.BatchMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchMorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchItemResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchAccountPredictionDTO;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.MonthlyHistory;
import com.naal.bankmind.entity.Default.DefaultPrediction;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
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

        guardarPrediccion(historial.get(0), response, account.getLimitBal());
        return response;
    }

    /**
     * Realiza predicciones batch para múltiples cuentas.
     * OPTIMIZADO: Solo 2 queries en lugar de N+1.
     */
    @Transactional
    public List<BatchAccountPredictionDTO> predecirBatch(List<Long> recordIds) {
        log.info("Iniciando predicción batch para {} cuentas", recordIds.size());

        if (recordIds.isEmpty()) {
            return new ArrayList<>();
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
            return new ArrayList<>();
        }

        log.info("Preparadas {} cuentas para predicción batch", apiRequests.size());

        BatchMorosidadRequestDTO batchRequest = new BatchMorosidadRequestDTO(apiRequests);
        BatchMorosidadResponseDTO batchResponse;
        try {
            batchResponse = morosidadClient.predictBatch(batchRequest);
            log.info("Respuesta batch recibida: {} predicciones", batchResponse.getTotalProcessed());
        } catch (Exception e) {
            log.error("Error en predicción batch: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio de predicción batch", e);
        }

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
                    batchResponse.getModelVersion());

            // Crear predicción sin guardar aún
            DefaultPrediction pred = crearPrediccion(historial.get(0), singleResponse, account.getLimitBal());
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
                    historial.get(0).getBillAmtX()));
        }

        // Guardar todas las predicciones en una sola operación batch
        if (!predictionsToSave.isEmpty()) {
            defaultPredictionRepository.saveAll(predictionsToSave);
            log.info("Guardadas {} predicciones en batch", predictionsToSave.size());
        }

        log.info("Predicción batch completada: {} resultados", results.size());
        return results;
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

        guardarPrediccion(historial.get(0), response, account.getLimitBal());

        int cuotasAtrasadas = calcularCuotasAtrasadas(historial);
        double historialPagos = calcularHistorialPagos(historial);
        int antiguedadMeses = calcularAntiguedad(customer);
        double probabilidadPago = (1.0 - response.probabilidadDefault()) * 100;
        String nivelRiesgo = calcularNivelRiesgo(probabilidadPago);
        BigDecimal estimatedLoss = calcularPerdidaEstimada(response, account.getLimitBal());

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
                response.mainRiskFactor(), response.modelVersion(), estimatedLoss);
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

    private BigDecimal calcularPerdidaEstimada(MorosidadResponseDTO response, BigDecimal limitBal) {
        if (response.isDefault() && limitBal != null) {
            return limitBal.multiply(BigDecimal.valueOf(response.probabilidadDefault()))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
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
     * Crea una predicción sin guardarla (para batch inserts).
     */
    private DefaultPrediction crearPrediccion(MonthlyHistory ultimoMes, MorosidadResponseDTO response,
            BigDecimal limitBal) {
        DefaultPrediction prediction = new DefaultPrediction();
        prediction.setMonthlyHistory(ultimoMes);
        prediction.setDatePrediction(LocalDateTime.now());
        prediction.setDefaultPaymentNextMonth(response.isDefault());
        prediction.setDefaultProbability(BigDecimal.valueOf(response.probabilidadDefault()));
        prediction.setMainRiskFactor(response.mainRiskFactor());

        if (response.isDefault() && limitBal != null) {
            BigDecimal estimatedLoss = limitBal.multiply(BigDecimal.valueOf(response.probabilidadDefault()));
            prediction.setEstimatedLoss(estimatedLoss.setScale(2, RoundingMode.HALF_UP));
        }
        return prediction;
    }

    /**
     * Guarda una predicción individual (para requests individuales).
     */
    private void guardarPrediccion(MonthlyHistory ultimoMes, MorosidadResponseDTO response, BigDecimal limitBal) {
        DefaultPrediction prediction = crearPrediccion(ultimoMes, response, limitBal);
        defaultPredictionRepository.save(prediction);
        log.info("Predicción guardada con ID: {}", prediction.getIdPrediction());
    }

    /**
     * Simula una predicción sin guardar en base de datos.
     */
    public com.naal.bankmind.dto.Default.Response.SimulationResponseDTO simulatePrediction(
            com.naal.bankmind.dto.Default.Request.SimulationRequestDTO request) {

        log.info("Simulando predicción de morosidad...");

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

        return new com.naal.bankmind.dto.Default.Response.SimulationResponseDTO(
                response.isDefault(),
                response.probabilidadDefault(),
                response.mainRiskFactor(),
                response.modelVersion());
    }
}
