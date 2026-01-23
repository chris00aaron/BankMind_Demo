package com.naal.bankmind.service.Default;

import com.naal.bankmind.client.Default.MorosidadFeignClient;
import com.naal.bankmind.dto.Default.Request.MorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.PredecirMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.DefaultPrediction;
import com.naal.bankmind.entity.MonthlyHistory;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.DefaultPredictionRepository;
import com.naal.bankmind.repository.Default.MonthlyHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para predicción de morosidad.
 * Arma el JSON con datos del cliente + historial, llama a la API y guarda la predicción.
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
     * 
     * @param request DTO con el recordId de la cuenta
     * @return Respuesta con predicción, probabilidad y factor de riesgo principal
     */
    @Transactional
    public MorosidadResponseDTO predecirMorosidad(PredecirMorosidadRequestDTO request) {
        log.info("Iniciando predicción de morosidad para recordId: {}", request.recordId());

        // 1. Obtener AccountDetails
        AccountDetails account = accountDetailsRepository.findById(request.recordId())
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + request.recordId()));

        Customer customer = account.getCustomer();
        log.info("Cliente encontrado: {} {}", customer.getFirstName(), customer.getSurname());

        // 2. Obtener últimos 6 meses de historial
        List<MonthlyHistory> historial = monthlyHistoryRepository
                .findTop6ByRecordIdOrderByMonthlyPeriodDesc(request.recordId());

        if (historial.size() < 6) {
            throw new RuntimeException("Se requieren al menos 6 meses de historial. Encontrados: " + historial.size());
        }

        // 3. Armar el JSON para la API
        MorosidadRequestDTO apiRequest = armarRequestAPI(account, customer, historial);

        // 4. Llamar a la API de predicción
        MorosidadResponseDTO response;
        try {
            response = morosidadClient.predict(apiRequest);
            log.info("Predicción recibida - Default: {}, Probabilidad: {}, Factor: {}",
                    response.isDefault(), response.probabilidadDefault(), response.mainRiskFactor());
        } catch (Exception e) {
            log.error("Error al obtener predicción de morosidad: {}", e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio de predicción de morosidad", e);
        }

        // 5. Guardar la predicción en BD
        guardarPrediccion(historial.get(0), response, account.getLimitBal());

        return response;
    }

    /**
     * Arma el DTO con los 24 campos requeridos por la API.
     */
    private MorosidadRequestDTO armarRequestAPI(AccountDetails account, Customer customer, List<MonthlyHistory> historial) {
        // Calcular UTILIZATION_RATE
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
                
                // PAY_0 a PAY_6 (índices 0-5 del historial)
                historial.get(0).getPayX() != null ? historial.get(0).getPayX() : 0,
                historial.get(1).getPayX() != null ? historial.get(1).getPayX() : 0,
                historial.get(2).getPayX() != null ? historial.get(2).getPayX() : 0,
                historial.get(3).getPayX() != null ? historial.get(3).getPayX() : 0,
                historial.get(4).getPayX() != null ? historial.get(4).getPayX() : 0,
                historial.get(5).getPayX() != null ? historial.get(5).getPayX() : 0,
                
                // BILL_AMT1 a BILL_AMT6
                historial.get(0).getBillAmtX() != null ? historial.get(0).getBillAmtX().doubleValue() : 0.0,
                historial.get(1).getBillAmtX() != null ? historial.get(1).getBillAmtX().doubleValue() : 0.0,
                historial.get(2).getBillAmtX() != null ? historial.get(2).getBillAmtX().doubleValue() : 0.0,
                historial.get(3).getBillAmtX() != null ? historial.get(3).getBillAmtX().doubleValue() : 0.0,
                historial.get(4).getBillAmtX() != null ? historial.get(4).getBillAmtX().doubleValue() : 0.0,
                historial.get(5).getBillAmtX() != null ? historial.get(5).getBillAmtX().doubleValue() : 0.0,
                
                // PAY_AMT1 a PAY_AMT6
                historial.get(0).getPayAmtX() != null ? historial.get(0).getPayAmtX().doubleValue() : 0.0,
                historial.get(1).getPayAmtX() != null ? historial.get(1).getPayAmtX().doubleValue() : 0.0,
                historial.get(2).getPayAmtX() != null ? historial.get(2).getPayAmtX().doubleValue() : 0.0,
                historial.get(3).getPayAmtX() != null ? historial.get(3).getPayAmtX().doubleValue() : 0.0,
                historial.get(4).getPayAmtX() != null ? historial.get(4).getPayAmtX().doubleValue() : 0.0,
                historial.get(5).getPayAmtX() != null ? historial.get(5).getPayAmtX().doubleValue() : 0.0,
                
                utilizationRate
        );
    }

    /**
     * Guarda la predicción en la base de datos.
     */
    private void guardarPrediccion(MonthlyHistory ultimoMes, MorosidadResponseDTO response, BigDecimal limitBal) {
        DefaultPrediction prediction = new DefaultPrediction();
        prediction.setMonthlyHistory(ultimoMes);
        prediction.setDatePrediction(LocalDateTime.now());
        prediction.setDefaultPaymentNextMonth(response.isDefault());
        prediction.setDefaultProbability(BigDecimal.valueOf(response.probabilidadDefault()));
        prediction.setMainRiskFactor(response.mainRiskFactor());
        prediction.setModelVersion(response.modelVersion());
        
        // Pérdida estimada = probabilidad * límite de crédito (simplificado)
        if (response.isDefault() && limitBal != null) {
            BigDecimal estimatedLoss = limitBal.multiply(BigDecimal.valueOf(response.probabilidadDefault()));
            prediction.setEstimatedLoss(estimatedLoss.setScale(2, RoundingMode.HALF_UP));
        }

        defaultPredictionRepository.save(prediction);
        log.info("Predicción guardada con ID: {}", prediction.getIdPrediction());
    }
}
