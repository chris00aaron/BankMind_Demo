package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.entity.FraudConfirmationToken;
import com.naal.bankmind.entity.OperationalTransactions;
import com.naal.bankmind.repository.Fraud.FraudConfirmationTokenRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio para confirmar/rechazar transacciones vía email
 */
@Slf4j
@Service
public class FraudConfirmationService {

    private final FraudConfirmationTokenRepository tokenRepository;
    private final TransactionRepository transactionRepository;
    private final CreditCardService creditCardService;

    public FraudConfirmationService(
            FraudConfirmationTokenRepository tokenRepository,
            TransactionRepository transactionRepository,
            CreditCardService creditCardService) {
        this.tokenRepository = tokenRepository;
        this.transactionRepository = transactionRepository;
        this.creditCardService = creditCardService;
    }

    /**
     * Cliente confirma que la transacción es legítima
     */
    @Transactional
    public String confirmLegitimate(String token) {
        // Validar token
        FraudConfirmationToken confirmToken = validateToken(token);

        // Obtener transacción
        OperationalTransactions transaction = confirmToken.getTransaction();

        // Marcar transacción como legítima
        transaction.setStatus("APPROVED");
        transaction.setIsFraudGroundTruth(0); // Feedback para ML: legítimo
        transactionRepository.save(transaction);

        // Marcar token como usado
        confirmToken.setIsUsed(true);
        confirmToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(confirmToken);

        log.info("Transacción {} confirmada como LEGÍTIMA por el cliente. is_fraud_ground_truth=0",
                transaction.getTransNum());

        return "Gracias por confirmar. La transacción ha sido aprobada.";
    }

    /**
     * Cliente reporta fraude - bloquear tarjeta y rechazar transacción
     */
    @Transactional
    public String blockCardAndReject(String token) {
        // Validar token
        FraudConfirmationToken confirmToken = validateToken(token);

        // Obtener transacción
        OperationalTransactions transaction = confirmToken.getTransaction();
        Long cardNumber = transaction.getCreditCard().getCcNum();

        // Marcar transacción como fraude
        transaction.setStatus("REJECTED");
        transaction.setIsFraudGroundTruth(1); // Feedback para ML: fraude confirmado
        transactionRepository.save(transaction);

        // Bloquear tarjeta
        creditCardService.blockCard(cardNumber);

        // Marcar token como usado
        confirmToken.setIsUsed(true);
        confirmToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(confirmToken);

        log.warn("Transacción {} marcada como FRAUDE. Tarjeta {} BLOQUEADA. is_fraud_ground_truth=1",
                transaction.getTransNum(),
                transaction.getCreditCard().getMaskedCardNumber());

        return "Tu tarjeta ha sido bloqueada por seguridad. Contacta a servicio al cliente para obtener una nueva.";
    }

    /**
     * Validar token (existencia, expiración, uso previo)
     */
    private FraudConfirmationToken validateToken(String token) {
        FraudConfirmationToken confirmToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no encontrado"));

        if (confirmToken.getIsUsed()) {
            throw new RuntimeException("Este enlace ya fue utilizado");
        }

        if (confirmToken.isExpired()) {
            throw new RuntimeException("Este enlace ha expirado. Contacta a servicio al cliente.");
        }

        return confirmToken;
    }
}
