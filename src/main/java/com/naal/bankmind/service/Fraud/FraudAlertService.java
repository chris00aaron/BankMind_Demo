package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.AuditDataDto;
import com.naal.bankmind.dto.Fraud.FraudAlertDto;
import com.naal.bankmind.dto.Fraud.FraudAlertsPageDto;
import com.naal.bankmind.dto.Fraud.RiskFactorDto;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Localization;
import com.naal.bankmind.entity.Fraud.FraudPredictions;
import com.naal.bankmind.entity.Fraud.OperationalTransactions;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio especializado en CONSULTA de alertas de fraude.
 *
 * Responsabilidad única: paginación, filtros y búsqueda de alertas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAlertService {

        private final FraudPredictionRepository fraudPredictionRepository;

        /**
         * Obtiene alertas de fraude paginadas con filtros y búsqueda.
         *
         * @param dateFilter "today" → día actual, "week" → últimos 7 días, null/"all" →
         *                   sin límite
         */
        @Transactional(readOnly = true)
        public FraudAlertsPageDto getAlertsPage(
                        int page, int size, String sortBy, String order,
                        String veredicto, String search, String dateFilter) {

                Sort.Direction direction = "asc".equalsIgnoreCase(order)
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC;
                String sortField = "score".equalsIgnoreCase(sortBy) ? "xgboostScore" : "predictionDate";
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

                boolean hasVeredicto = veredicto != null && !veredicto.isEmpty()
                                && !"TODO".equalsIgnoreCase(veredicto);
                boolean hasSearch = search != null && !search.isEmpty();
                boolean hasDateFilter = dateFilter != null && !dateFilter.isEmpty()
                                && !"all".equalsIgnoreCase(dateFilter);

                Page<FraudPredictions> resultsPage;

                if (hasDateFilter) {
                        LocalDateTime dateTo = LocalDateTime.now().with(LocalTime.MAX);
                        LocalDateTime dateFrom = "today".equalsIgnoreCase(dateFilter)
                                        ? LocalDateTime.now().with(LocalTime.MIN)
                                        : LocalDateTime.now().minusDays(7).with(LocalTime.MIN);

                        if (hasVeredicto && hasSearch) {
                                resultsPage = fraudPredictionRepository
                                                .searchByDateRangeVeredictoAndTransactionId(
                                                                dateFrom, dateTo, veredicto, search, pageable);
                        } else if (hasVeredicto) {
                                resultsPage = fraudPredictionRepository
                                                .findByDateRangeAndVeredicto(dateFrom, dateTo, veredicto, pageable);
                        } else if (hasSearch) {
                                resultsPage = fraudPredictionRepository
                                                .searchByDateRangeAndTransactionId(dateFrom, dateTo, search, pageable);
                        } else {
                                resultsPage = fraudPredictionRepository
                                                .findByDateRangePaged(dateFrom, dateTo, pageable);
                        }
                } else {
                        // Sin filtro de fecha — comportamiento original
                        if (hasVeredicto && hasSearch) {
                                resultsPage = fraudPredictionRepository
                                                .searchByVeredictoAndTransactionId(veredicto, search, pageable);
                        } else if (hasVeredicto) {
                                resultsPage = fraudPredictionRepository
                                                .findByVeredictoWithDetailsPaged(veredicto, pageable);
                        } else if (hasSearch) {
                                resultsPage = fraudPredictionRepository.searchByTransactionId(search, pageable);
                        } else {
                                resultsPage = fraudPredictionRepository.findAllWithDetailsPaged(pageable);
                        }
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
         * Obtiene el detalle completo de una alerta por su ID.
         */
        @Transactional(readOnly = true)
        public FraudAlertDto getAlertDetail(Long predictionId) {
                FraudPredictions prediction = fraudPredictionRepository
                                .findByIdWithDetails(predictionId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Alerta no encontrada con ID: " + predictionId));
                return mapToAlertDtoWithDetails(prediction);
        }

        // ─── Mapeo ────────────────────────────────────────────────────────────────

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

                String recomendacion = FraudConstants.VEREDICTO_ALTO_RIESGO.equals(prediction.getVeredicto())
                                ? FraudConstants.RECOMENDACION_BLOQUEAR
                                : FraudConstants.RECOMENDACION_APROBAR;

                return FraudAlertDto.builder()
                                .predictionId(prediction.getIdFraudPrediction())
                                .transactionId(transaction != null ? transaction.getTransNum() : null)
                                .transactionDbId(transaction != null ? transaction.getIdTransaction() : null)
                                .veredicto(prediction.getVeredicto())
                                .scoreFinal(prediction.getXgboostScore())
                                .amount(transaction != null ? transaction.getAmt().doubleValue() : null)
                                .merchant(transaction != null ? transaction.getMerchant() : null)
                                .category(transaction != null ? transaction.getCategoryName() : null)
                                .customerName(customer != null
                                                ? customer.getFirstName() + " " + customer.getSurname()
                                                : null)
                                .predictionDate(prediction.getPredictionDate())
                                .location(location)
                                .recomendacion(recomendacion)
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
