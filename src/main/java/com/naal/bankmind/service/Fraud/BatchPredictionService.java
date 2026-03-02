package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.BatchApiResponseDto;
import com.naal.bankmind.dto.Fraud.BatchResultDto;
import com.naal.bankmind.dto.Fraud.BatchResultDto.BatchItemResultDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionRequestDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionResponseDto;
import com.naal.bankmind.dto.Fraud.PendingTransactionDto;
import com.naal.bankmind.entity.Fraud.CreditCards;
import com.naal.bankmind.entity.Fraud.OperationalTransactions;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para procesamiento de predicciones por lotes.
 *
 * ─── Diseño para producción ────────────────────────────────────────────────
 * El flujo se divide en tres fases con ciclos de transacción independientes:
 *
 * FASE 1 — {@link #loadTransactionsBulk}: una query IN (ids) carga y verifica
 * todas las transacciones. Se hace readOnly para minimizar lock time.
 *
 * FASE 2 — {@link #callBatchApi}: llamada HTTP a la API Python. Sin
 * 
 * @Transactional → la conexión del pool NO permanece abierta
 *                durante los 10-30 segundos que puede tardar un batch.
 *
 *                FASE 3 — {@link #savePredictions}: guarda las predicciones en
 *                una
 *                transacción corta y dedicada.
 *
 *                processNextBatch() NO propaga su transacción a processBatch()
 *                porque
 *                cada fase gestiona la suya propia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPredictionService {

    private final TransactionRepository transactionRepository;
    private final FraudPredictionRepository fraudPredictionRepository;
    private final FraudApiClient fraudApiClient;
    private final PredictionMapper predictionMapper;

    private static final int MAX_BATCH_SIZE = 100;

    // ─── API pública ────────────────────────────────────────────────────────

    /**
     * Obtiene el conteo de transacciones pendientes de análisis.
     */
    public long getPendingCount() {
        return transactionRepository.countPendingPredictions();
    }

    /**
     * Lista transacciones pendientes de análisis (para mostrar en UI).
     */
    @Transactional(readOnly = true)
    public List<PendingTransactionDto> getPendingTransactions(int limit) {
        int size = Math.min(limit, MAX_BATCH_SIZE);
        return transactionRepository
                .findPendingPredictions(PageRequest.of(0, size))
                .stream()
                .map(this::mapToPendingDto)
                .collect(Collectors.toList());
    }

    /**
     * Procesa un lote de transacciones por sus IDs.
     *
     * Sin @Transactional propio: la transacción de BD se abre y cierra
     * dentro de cada fase (loadTransactionsBulk y savePredictions).
     * La llamada HTTP (callBatchApi) queda completamente fuera de cualquier
     * transacción activa, liberando el pool durante la espera de red.
     */
    public BatchResultDto processBatch(List<Long> transactionIds) {
        int size = Math.min(transactionIds.size(), MAX_BATCH_SIZE);
        List<Long> idsToProcess = transactionIds.subList(0, size);

        List<BatchItemResultDto> results = new ArrayList<>();

        // FASE 1: cargar datos en bulk y separar "ya procesados" de "a procesar"
        BatchLoad loaded = loadTransactionsBulk(idsToProcess, results);

        if (loaded.apiRequests().isEmpty()) {
            // Todos already o no encontrados — nada que llamar
            return buildSummary(results);
        }

        // FASE 2: llamar API de IA sin transacción de BD abierta
        BatchApiResponseDto apiResponse = callBatchApi(loaded, results);

        if (apiResponse == null) {
            return buildSummary(results);
        }

        // FASE 3: persistir predicciones en transacción corta y separada
        savePredictions(apiResponse, loaded.transactionsMap(), loaded.transNumToIdMap(), results);

        return buildSummary(results);
    }

    /**
     * Procesa automáticamente las siguientes N transacciones pendientes.
     *
     * Fix B5: NO lleva @Transactional propio porque processBatch() ya gestiona
     * sus propias transacciones por fase. La carga de IDs pendientes ocurre en
     * su propia transacción readOnly, aislada del procesamiento del batch.
     */
    public BatchResultDto processNextBatch(int limit) {
        int size = Math.min(limit, MAX_BATCH_SIZE);
        List<Long> pendingIds = loadPendingIds(size);
        if (pendingIds.isEmpty()) {
            log.info("No hay transacciones pendientes de análisis.");
            return BatchResultDto.builder()
                    .totalProcessed(0).totalFrauds(0)
                    .totalLegitimate(0).totalErrors(0)
                    .results(List.of())
                    .build();
        }
        return processBatch(pendingIds);
    }

    // ─── Fases internas ──────────────────────────────────────────────────────

    /**
     * FASE 0: Carga IDs pendientes en una transacción readOnly corta.
     */
    @Transactional(readOnly = true)
    protected List<Long> loadPendingIds(int size) {
        return transactionRepository.findPendingPredictionIds(PageRequest.of(0, size));
    }

    /**
     * FASE 1: Carga todas las transacciones y filtra las ya procesadas.
     *
     * Fix B2: una sola query findExistingTransactionIds() en lugar de N
     * llamadas a existsByTransactionIdTransaction().
     * Fix B3: una sola query findAllByIdsWithCustomerData() en lugar de N
     * llamadas a findByIdWithCustomerData().
     */
    @Transactional(readOnly = true)
    protected BatchLoad loadTransactionsBulk(List<Long> idsToProcess,
            List<BatchItemResultDto> results) {
        // B2: verificación masiva en una sola query
        Set<Long> alreadyPredicted = fraudPredictionRepository
                .findExistingTransactionIds(idsToProcess);

        List<Long> idsNeedingPrediction = new ArrayList<>();
        for (Long id : idsToProcess) {
            if (alreadyPredicted.contains(id)) {
                results.add(errorItem(id, "Ya tiene predicción"));
            } else {
                idsNeedingPrediction.add(id);
            }
        }

        Map<Long, OperationalTransactions> transactionsMap = new HashMap<>();
        List<FraudPredictionRequestDto> apiRequests = new ArrayList<>();
        Map<String, Long> transNumToIdMap = new HashMap<>();

        if (!idsNeedingPrediction.isEmpty()) {
            // B3: carga masiva en una sola query con JOIN FETCH
            List<OperationalTransactions> loaded = transactionRepository
                    .findAllByIdsWithCustomerData(idsNeedingPrediction);

            Set<Long> loadedIds = loaded.stream()
                    .map(OperationalTransactions::getIdTransaction)
                    .collect(Collectors.toSet());

            // Marcar los que no se encontraron
            for (Long id : idsNeedingPrediction) {
                if (!loadedIds.contains(id)) {
                    results.add(errorItem(id, "Transacción no encontrada"));
                }
            }

            for (OperationalTransactions tx : loaded) {
                transactionsMap.put(tx.getIdTransaction(), tx);
                apiRequests.add(predictionMapper.buildApiRequest(tx));
                transNumToIdMap.put(tx.getTransNum(), tx.getIdTransaction());
            }
        }

        log.debug("Batch load: {} a procesar, {} ya predichos, {} no encontrados",
                transactionsMap.size(),
                alreadyPredicted.size(),
                idsToProcess.size() - alreadyPredicted.size() - transactionsMap.size());

        return new BatchLoad(transactionsMap, apiRequests, transNumToIdMap);
    }

    /**
     * FASE 2: Llama a la API de IA.
     *
     * Fix B1: sin @Transactional → la conexión del pool se libera antes de
     * la llamada HTTP, que puede tardar 10-30 segundos.
     * Fix B4: el mensaje de excepción NO se expone al cliente; solo va al log.
     */
    private BatchApiResponseDto callBatchApi(BatchLoad loaded,
            List<BatchItemResultDto> results) {
        try {
            return fraudApiClient.predictFraudBatch(loaded.apiRequests());
        } catch (Exception e) {
            // B4: log con detalle completo, mensaje genérico al cliente
            log.error("Error en llamada batch a API de IA. " +
                    "Transacciones afectadas: {}. Causa: {}",
                    loaded.transactionsMap().keySet(), e.getMessage(), e);

            // Fix B6 (documentado): solo las IDs que llegaron al map se marcan aquí;
            // las que fallaron antes (no encontradas, ya predichas) ya están en results.
            for (Long id : loaded.transactionsMap().keySet()) {
                results.add(errorItem(id, "Error al contactar el servicio de análisis de fraude"));
            }
            return null;
        }
    }

    /**
     * FASE 3: Persiste las predicciones recibidas de la API.
     * Transacción corta, independiente de la fase de carga.
     */
    @Transactional
    protected void savePredictions(BatchApiResponseDto batchResponse,
            Map<Long, OperationalTransactions> transactionsMap,
            Map<String, Long> transNumToIdMap,
            List<BatchItemResultDto> results) {
        for (FraudPredictionResponseDto apiResponse : batchResponse.getResults()) {
            Long id = transNumToIdMap.get(apiResponse.getTransactionId());
            OperationalTransactions tx = transactionsMap.get(id);

            boolean valid = tx != null
                    && apiResponse.getVeredicto() != null
                    && !"ERROR".equals(apiResponse.getVeredicto());

            if (valid) {
                predictionMapper.savePrediction(tx, apiResponse, FraudConstants.SCENARIO_BATCH);
                results.add(BatchItemResultDto.builder()
                        .idTransaction(id)
                        .transNum(tx.getTransNum())
                        .amt(tx.getAmt().doubleValue())
                        .veredicto(apiResponse.getVeredicto())
                        .score(apiResponse.getScoreFinal())
                        .status("success") // El frontend verifica r.status === 'success'
                        .build());
            } else {
                String msg = (apiResponse.getError() != null && !apiResponse.getError().isBlank())
                        ? apiResponse.getError()
                        : "Error en predicción individual";
                results.add(errorItem(id, msg));
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private BatchResultDto buildSummary(List<BatchItemResultDto> results) {
        long frauds = results.stream()
                .filter(r -> "success".equals(r.getStatus())
                        && FraudConstants.VEREDICTO_ALTO_RIESGO.equals(r.getVeredicto()))
                .count();
        long legit = results.stream()
                .filter(r -> "success".equals(r.getStatus())
                        && !FraudConstants.VEREDICTO_ALTO_RIESGO.equals(r.getVeredicto()))
                .count();
        long errors = results.stream()
                .filter(r -> !"success".equals(r.getStatus()))
                .count();
        log.info("Batch completado: procesadas={}, fraudes={}, legítimas={}, errores={}",
                results.size(), frauds, legit, errors);
        return BatchResultDto.builder()
                .totalProcessed(results.size())
                .totalFrauds((int) frauds)
                .totalLegitimate((int) legit)
                .totalErrors((int) errors)
                .results(results)
                .build();
    }

    private PendingTransactionDto mapToPendingDto(OperationalTransactions t) {
        CreditCards cc = t.getCreditCard();
        Customer customer = cc != null ? cc.getCustomer() : null;

        String ccNumMasked = cc != null && cc.getCcNum() != null
                ? "****" + String.valueOf(cc.getCcNum())
                        .substring(Math.max(0, String.valueOf(cc.getCcNum()).length() - 4))
                : null;

        return PendingTransactionDto.builder()
                .idTransaction(t.getIdTransaction())
                .transNum(t.getTransNum())
                .transDateTime(t.getTransDateTime())
                .amt(t.getAmt() != null ? t.getAmt().doubleValue() : null)
                .category(t.getCategoryName())
                .merchant(t.getMerchant())
                .customerName(customer != null
                        ? customer.getFirstName() + " " + customer.getSurname()
                        : null)
                .ccNumMasked(ccNumMasked)
                .build();
    }

    private static BatchItemResultDto errorItem(Long id, String message) {
        return BatchItemResultDto.builder()
                .idTransaction(id)
                .status("error")
                .errorMessage(message)
                .build();
    }

    // ─── Tipos auxiliares ─────────────────────────────────────────────────────

    /**
     * Resultado de FASE 1: agrupa en un solo objeto todo lo necesario para
     * las fases 2 y 3 sin usar tipos anónimos ni Object[].
     *
     * Fix B6 (documentado): transactionsMap contiene SOLO las transacciones
     * que superaron los filtros (existencia + sin predicción previa). Las que
     * fallaron antes ya están anotadas en results con su estado correcto.
     */
    private record BatchLoad(
            Map<Long, OperationalTransactions> transactionsMap,
            List<FraudPredictionRequestDto> apiRequests,
            Map<String, Long> transNumToIdMap) {
    }
}
