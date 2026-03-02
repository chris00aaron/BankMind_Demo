package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.FraudPredictionRequestDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionResponseDto;
import com.naal.bankmind.dto.Fraud.NewTransactionRequestDto;
import com.naal.bankmind.dto.Fraud.TransactionResultDto;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Fraud.Category;
import com.naal.bankmind.entity.Fraud.CreditCards;
import com.naal.bankmind.entity.Fraud.OperationalTransactions;
import com.naal.bankmind.entity.Localization;
import com.naal.bankmind.repository.Fraud.CategoryRepository;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio que orquesta el flujo completo de procesamiento de transacciones:
 * 1. Valida tarjeta y cliente
 * 2. Enriquece datos
 * 3. Guarda la transacción (COMMIT antes de llamar a la API)
 * 4. Llama a la API de IA — FUERA de cualquier transacción abierta
 * 5. Guarda la predicción en una transacción corta independiente
 *
 * ─── Por qué este diseño ────────────────────────────────────────────────────
 * La anotación @Transactional NO puede envolver llamadas HTTP externas en
 * producción: mantendría una conexión del pool abierta durante toda la
 * latencia de red (500 ms–2 s) agotando el pool con cargas concurrentes.
 *
 * Se usan tres métodos independientes con su propio ciclo de transacción:
 * ┌──────────────────────────────────────────────────────────┐
 * │ saveTransaction() → guardar + COMMIT → liberar pool │
 * │ callFraudApi() → HTTP externo (sin transacción) │
 * │ savePrediction() → guardar + COMMIT (si hubo resp.) │
 * └──────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CreditCardRepository creditCardRepository;
    private final CategoryRepository categoryRepository;
    private final FraudPredictionService fraudPredictionService;
    private final PredictionMapper predictionMapper; // Fix #15: elimina extractGenderCode duplicado

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─── Punto de entrada público ──────────────────────────────────────────────

    /**
     * Procesa una nueva transacción desde el POS.
     *
     * Nota: este método NO lleva @Transactional propio — la transacción de BD
     * se abre y cierra dentro de {@link #saveTransaction} y luego (si procede)
     * dentro de {@link #savePredictionInternal}. Así la conexión del pool se
     * libera antes de que empiece la llamada HTTP a la API de IA.
     */
    public TransactionResultDto processNewTransaction(NewTransactionRequestDto request) {
        long startTime = System.currentTimeMillis();

        // FASE 1 — Guardar transacción (transacción corta, libera conexión al terminar)
        SavedTransaction saved = saveTransaction(request);

        // FASE 2 — Llamar API de IA (sin transacción de BD abierta)
        FraudPredictionResponseDto fraudResponse = callFraudApi(saved);

        // FASE 3 — Guardar predicción si la API respondió correctamente
        if (fraudResponse != null) {
            savePredictionInternal(saved.transaction(), fraudResponse);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        String status = fraudResponse != null ? "PROCESSED" : "PENDING_ANALYSIS";
        return buildResult(saved.transaction(), saved.customer(), fraudResponse, processingTime, status);
    }

    // ─── Fases independientes ──────────────────────────────────────────────────

    /**
     * FASE 1: Valida tarjeta, enriquece datos y persiste la transacción.
     * La transacción de BD se cierra (COMMIT) al salir de este método.
     */
    @Transactional
    protected SavedTransaction saveTransaction(NewTransactionRequestDto request) {
        // 1. Identificar cliente por número de tarjeta
        CreditCards creditCard = creditCardRepository.findById(request.getCcNum())
                .orElseThrow(() -> new RuntimeException(
                        "Tarjeta no encontrada: " + request.getCcNum()));

        if (creditCard.getIsActive() == null || !creditCard.getIsActive()) {
            throw new RuntimeException(
                    "TARJETA BLOQUEADA: Esta tarjeta ha sido bloqueada por seguridad. " +
                            "No se pueden procesar transacciones. " +
                            "Contacte a servicio al cliente: 1-800-BANKMIND");
        }

        Customer customer = creditCard.getCustomer();
        if (customer == null) {
            throw new RuntimeException("No se encontró cliente para la tarjeta");
        }

        // 2. Crear y enriquecer transacción
        OperationalTransactions transaction = new OperationalTransactions();
        transaction.setCreditCard(creditCard);
        transaction.setTransNum(generateTransactionNumber());
        transaction.setTransDateTime(LocalDateTime.now());
        transaction.setAmt(BigDecimal.valueOf(request.getAmt()));
        transaction.setMerchant(request.getMerchant());

        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            Category category = categoryRepository.findByCategoryName(request.getCategory())
                    .orElseThrow(() -> new RuntimeException(
                            "Categoría no encontrada: " + request.getCategory()));
            transaction.setCategory(category);
        }

        transaction.setMerchLat(request.getMerchLat());
        transaction.setMerchLong(request.getMerchLong());
        transaction.setUnixTime(System.currentTimeMillis() / 1000);
        transaction.setIsFraudGroundTruth(null);

        // 3. Persistir — el COMMIT ocurre al salir del método (@Transactional)
        OperationalTransactions savedTx = transactionRepository.save(transaction);
        log.debug("Transacción {} guardada (id={})", savedTx.getTransNum(), savedTx.getIdTransaction());
        return new SavedTransaction(savedTx, customer);
    }

    /**
     * FASE 2: Llama a la API de IA. Sin @Transactional — no mantiene conexión de
     * BD.
     * Devuelve null si la API no responde y loggea el error con suficiente detalle.
     */
    private FraudPredictionResponseDto callFraudApi(SavedTransaction saved) {
        FraudPredictionRequestDto fraudRequest = buildFraudRequest(saved.transaction(), saved.customer());
        try {
            return fraudPredictionService.predictTransactionDirect(fraudRequest, saved.transaction());
        } catch (Exception e) {
            // Fix #14: log obligatorio — en producción permite diagnóstico y alertas
            log.warn("API de fraude no disponible para transacción {} (id={}). " +
                    "Quedará en estado PENDING_ANALYSIS para reprocesamiento. Causa: {}",
                    saved.transaction().getTransNum(),
                    saved.transaction().getIdTransaction(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * FASE 3: Guarda la predicción en una transacción de BD corta e independiente.
     * Solo se invoca si la API respondió correctamente.
     */
    @Transactional
    protected void savePredictionInternal(OperationalTransactions transaction,
            FraudPredictionResponseDto response) {
        predictionMapper.savePrediction(transaction, response, FraudConstants.SCENARIO_INDIVIDUAL);
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Construye el request para la API usando PredictionMapper (Fix #15).
     * PredictionMapper.buildApiRequest() ya resuelve el cliente a través de
     * la relación transaction → creditCard → customer, incluyendo el idCliente.
     */
    private FraudPredictionRequestDto buildFraudRequest(OperationalTransactions transaction,
            @SuppressWarnings("unused") Customer customer) {
        // La relación creditCard.customer ya está cargada en saveTransaction().
        // PredictionMapper resuelve todos los campos incluyendo idCliente.
        return predictionMapper.buildApiRequest(transaction);
    }

    private String generateTransactionNumber() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TransactionResultDto buildResult(
            OperationalTransactions transaction,
            Customer customer,
            FraudPredictionResponseDto fraudAnalysis,
            long processingTime,
            String status) {

        return TransactionResultDto.builder()
                .transactionId(transaction.getIdTransaction())
                .transNum(transaction.getTransNum())
                .amt(transaction.getAmt().doubleValue())
                .merchant(transaction.getMerchant())
                .category(transaction.getCategoryName())
                .transDateTime(transaction.getTransDateTime())
                .customerName(customer.getFirstName() + " " + customer.getSurname())
                .customerId(customer.getIdCustomer())
                .fraudAnalysis(fraudAnalysis)
                .processingTimeMs(processingTime)
                .status(status)
                .build();
    }

    // ─── Tipos auxiliares ──────────────────────────────────────────────────────

    /**
     * Par (transacción guardada, cliente) retornado por saveTransaction().
     * Encapsula los dos objetos necesarios para las fases 2 y 3 sin usar Object[].
     */
    private record SavedTransaction(OperationalTransactions transaction, Customer customer) {
    }
}
