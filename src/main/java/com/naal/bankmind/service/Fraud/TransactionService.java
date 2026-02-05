package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.entity.*;
import com.naal.bankmind.repository.Fraud.CategoryRepository;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio que orquesta el flujo completo de procesamiento de transacciones:
 * 1. Recibe datos mínimos del POS
 * 2. Identifica al cliente por número de tarjeta
 * 3. Enriquece con datos del cliente (género, trabajo, ubicación, etc.)
 * 4. Guarda la transacción en la BD
 * 5. Llama a la API de IA para evaluar fraude
 * 6. Guarda la predicción y devuelve resultado unificado
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CreditCardRepository creditCardRepository;
    private final CategoryRepository categoryRepository;
    private final FraudPredictionService fraudPredictionService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TransactionService(
            TransactionRepository transactionRepository,
            CreditCardRepository creditCardRepository,
            CategoryRepository categoryRepository,
            FraudPredictionService fraudPredictionService) {
        this.transactionRepository = transactionRepository;
        this.creditCardRepository = creditCardRepository;
        this.categoryRepository = categoryRepository;
        this.fraudPredictionService = fraudPredictionService;
    }

    /**
     * Procesa una nueva transacción desde el POS
     * - Enriquece datos
     * - Guarda transacción
     * - Evalúa fraude automáticamente
     * 
     * @param request Datos mínimos del punto de venta
     * @return Resultado con transacción + análisis de fraude
     */
    @Transactional
    public TransactionResultDto processNewTransaction(NewTransactionRequestDto request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. IDENTIFICAR CLIENTE por número de tarjeta
            CreditCards creditCard = creditCardRepository.findById(request.getCcNum())
                    .orElseThrow(() -> new RuntimeException(
                            "Tarjeta no encontrada: " + request.getCcNum()));

            // VALIDACIÓN CRÍTICA: Verificar que la tarjeta esté activa
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

            // 2. CREAR Y ENRIQUECER TRANSACCIÓN
            OperationalTransactions transaction = new OperationalTransactions();
            transaction.setCreditCard(creditCard);
            transaction.setTransNum(generateTransactionNumber());
            transaction.setTransDateTime(LocalDateTime.now());
            transaction.setAmt(BigDecimal.valueOf(request.getAmt()));
            transaction.setMerchant(request.getMerchant());

            // Buscar categoría por nombre y asignar la entidad
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                Category category = categoryRepository.findByCategoryName(request.getCategory())
                        .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + request.getCategory()));
                transaction.setCategory(category);
            }

            transaction.setMerchLat(request.getMerchLat());
            transaction.setMerchLong(request.getMerchLong());
            transaction.setUnixTime(System.currentTimeMillis() / 1000);
            transaction.setIsFraudGroundTruth(null); // Se determinará después

            // 3. GUARDAR TRANSACCIÓN
            OperationalTransactions savedTransaction = transactionRepository.save(transaction);

            // 4. PREPARAR REQUEST PARA API DE FRAUDE (con datos enriquecidos)
            FraudPredictionRequestDto fraudRequest = buildFraudRequest(savedTransaction, customer);

            // 5. LLAMAR A LA API DE IA
            FraudPredictionResponseDto fraudResponse;
            try {
                fraudResponse = fraudPredictionService.predictTransactionDirect(fraudRequest, savedTransaction);
            } catch (Exception e) {
                // Si falla la API, devolvemos transacción sin predicción
                return buildResult(savedTransaction, customer, null,
                        System.currentTimeMillis() - startTime, "PENDING_ANALYSIS");
            }

            // 6. CONSTRUIR Y DEVOLVER RESULTADO UNIFICADO
            long processingTime = System.currentTimeMillis() - startTime;
            return buildResult(savedTransaction, customer, fraudResponse, processingTime, "PROCESSED");

        } catch (Exception e) {
            throw new RuntimeException("Error procesando transacción: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un número de transacción único
     */
    private String generateTransactionNumber() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Construye el request para la API de fraude con datos enriquecidos del cliente
     */
    private FraudPredictionRequestDto buildFraudRequest(OperationalTransactions transaction, Customer customer) {
        Localization location = customer.getLocalization();
        Gender gender = customer.getGender();

        return FraudPredictionRequestDto.builder()
                .transactionId(transaction.getTransNum())
                .idCliente(customer.getIdCustomer().toString())
                .transDateTransTime(transaction.getTransDateTime().format(DATE_TIME_FORMATTER))
                .amt(transaction.getAmt().doubleValue())
                .category(transaction.getCategoryName())
                .gender(extractGenderCode(gender))
                .job(customer.getJob() != null ? customer.getJob() : "Unknown")
                .cityPop(location != null ? location.getCityPop() : 0)
                .dob(customer.getDob() != null ? customer.getDob().format(DATE_FORMATTER) : "1990-01-01")
                .lat(location != null ? location.getCustomerLat() : 0.0)
                .lng(location != null ? location.getCustomerLong() : 0.0)
                .merchLat(transaction.getMerchLat() != null ? transaction.getMerchLat() : 0.0)
                .merchLong(transaction.getMerchLong() != null ? transaction.getMerchLong() : 0.0)
                .build();
    }

    /**
     * Extrae código de género (M/F) del objeto Gender
     */
    private String extractGenderCode(Gender gender) {
        if (gender == null)
            return "M";
        String genderDesc = gender.getGenderDescription();
        if (genderDesc != null) {
            genderDesc = genderDesc.toLowerCase();
            if (genderDesc.contains("female") || genderDesc.contains("femenino") || genderDesc.startsWith("f")) {
                return "F";
            }
        }
        return "M";
    }

    /**
     * Construye el DTO de resultado unificado
     */
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
}
