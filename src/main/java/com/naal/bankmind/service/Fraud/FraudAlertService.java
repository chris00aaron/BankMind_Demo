package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.entity.*;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio especializado en CONSULTA de alertas de fraude
 * Responsabilidad: Paginación, filtros, búsqueda de alertas
 * 
 * Principio SOLID: Single Responsibility
 */
@Service
public class FraudAlertService {

    private final FraudPredictionRepository fraudPredictionRepository;

    public FraudAlertService(FraudPredictionRepository fraudPredictionRepository) {
        this.fraudPredictionRepository = fraudPredictionRepository;
    }

    /**
     * Obtiene alertas de fraude paginadas con filtros y búsqueda
     */
    @Transactional(readOnly = true)
    public FraudAlertsPageDto getAlertsPage(
            int page, int size, String sortBy, String order,
            String veredicto, String search) {

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = "score".equalsIgnoreCase(sortBy) ? "xgboostScore" : "predictionDate";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<FraudPredictions> resultsPage;

        boolean hasVeredicto = veredicto != null && !veredicto.isEmpty() && !"TODO".equalsIgnoreCase(veredicto);
        boolean hasSearch = search != null && !search.isEmpty();

        if (hasVeredicto && hasSearch) {
            resultsPage = fraudPredictionRepository.searchByVeredictoAndTransactionId(veredicto, search, pageable);
        } else if (hasVeredicto) {
            resultsPage = fraudPredictionRepository.findByVeredictoWithDetailsPaged(veredicto, pageable);
        } else if (hasSearch) {
            resultsPage = fraudPredictionRepository.searchByTransactionId(search, pageable);
        } else {
            resultsPage = fraudPredictionRepository.findAllWithDetailsPaged(pageable);
        }

        List<FraudAlertDto> alerts = resultsPage.getContent().stream()
                .map(this::mapToAlertDto)
                .collect(Collectors.toList());

        return FraudAlertsPageDto.builder()
                .content(alerts)
                .page(resultsPage.getNumber())
                .size(resultsPage.getSize())
                .totalElements(resultsPage.getTotalElements())
                .totalPages(resultsPage.getTotalPages())
                .hasNext(resultsPage.hasNext())
                .hasPrevious(resultsPage.hasPrevious())
                .build();
    }

    /**
     * Obtiene el detalle completo de una alerta por su ID
     */
    @Transactional(readOnly = true)
    public FraudAlertDto getAlertDetail(Long predictionId) {
        FraudPredictions prediction = fraudPredictionRepository.findByIdWithDetails(predictionId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada con ID: " + predictionId));
        return mapToAlertDtoWithDetails(prediction);
    }

    // ================== MAPEO ==================

    private FraudAlertDto mapToAlertDto(FraudPredictions prediction) {
        OperationalTransactions transaction = prediction.getTransaction();
        Customer customer = null;
        String location = null;

        if (transaction != null && transaction.getCreditCard() != null) {
            customer = transaction.getCreditCard().getCustomer();
            if (customer != null && customer.getLocalization() != null) {
                Localization loc = customer.getLocalization();
                location = loc.getCity() + ", " + loc.getState();
            }
        }

        return FraudAlertDto.builder()
                .predictionId(prediction.getIdFraudPrediction())
                .transactionId(transaction != null ? transaction.getTransNum() : null)
                .transactionDbId(transaction != null ? transaction.getIdTransaction() : null)
                .veredicto(prediction.getVeredicto())
                .scoreFinal(prediction.getXgboostScore())
                .amount(transaction != null ? transaction.getAmt().doubleValue() : null)
                .merchant(transaction != null ? transaction.getMerchant() : null)
                .category(transaction != null ? transaction.getCategoryName() : null)
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getSurname() : null)
                .predictionDate(prediction.getPredictionDate())
                .location(location)
                .recomendacion("ALTO RIESGO".equals(prediction.getVeredicto()) ? "Bloquear y Notificar" : "Aprobar")
                .build();
    }

    private FraudAlertDto mapToAlertDtoWithDetails(FraudPredictions prediction) {
        FraudAlertDto alert = mapToAlertDto(prediction);

        if (prediction.getDetails() != null) {
            List<RiskFactorDto> riskFactors = prediction.getDetails().stream()
                    .map(d -> RiskFactorDto.builder()
                            .featureName(d.getFeatureName())
                            .featureValue(d.getFeatureValue())
                            .shapValue(d.getShapValue())
                            .riskDescription(d.getRiskDescription())
                            .impactDirection(d.getImpactDirection())
                            .build())
                    .collect(Collectors.toList());
            alert.setDetallesRiesgo(riskFactors);
        }

        alert.setDatosAuditoria(AuditDataDto.builder()
                .xgboostScore(prediction.getXgboostScore())
                .iforestScore(prediction.getIfforestScore())
                .baseScore(0.151f)
                .predictionId(prediction.getIdFraudPrediction())
                .build());

        return alert;
    }
}
