package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.entity.FraudConfirmationToken;
import com.naal.bankmind.entity.FraudPredictions;
import com.naal.bankmind.entity.OperationalTransactions;
import com.naal.bankmind.repository.Fraud.FraudConfirmationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio para envío de emails de alerta de fraude
 * Usa EmailService genérico para el envío real
 * Responsabilidad: Lógica específica de fraude (tokens, templates, datos)
 */
@Slf4j
@Service
public class FraudEmailService {

        private final com.naal.bankmind.service.email.EmailService emailService;
        private final FraudConfirmationTokenRepository tokenRepository;

        @Value("${bankmind.email.confirmation-url}")
        private String confirmationBaseUrl;

        @Value("${bankmind.email.token-expiry-hours}")
        private int tokenExpiryHours;

        public FraudEmailService(
                        com.naal.bankmind.service.email.EmailService emailService,
                        FraudConfirmationTokenRepository tokenRepository) {
                this.emailService = emailService;
                this.tokenRepository = tokenRepository;
        }

        /**
         * Enviar email de alerta de fraude al cliente
         */
        @Transactional
        public void sendFraudAlert(OperationalTransactions transaction, FraudPredictions prediction) {
                try {
                        String customerEmail = transaction.getCreditCard().getCustomer().getEmail();

                        if (!emailService.isValidEmail(customerEmail)) {
                                log.warn("Cliente sin email válido. No se puede enviar alerta. Transacción: {}",
                                                transaction.getTransNum());
                                return;
                        }

                        // Generar token único
                        String token = generateSecureToken();
                        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpiryHours);

                        // Guardar token en base de datos
                        FraudConfirmationToken confirmToken = new FraudConfirmationToken();
                        confirmToken.setToken(token);
                        confirmToken.setTransaction(transaction);
                        confirmToken.setPrediction(prediction);
                        confirmToken.setTokenType("CONFIRM_OR_BLOCK");
                        confirmToken.setCreatedAt(LocalDateTime.now());
                        confirmToken.setExpiresAt(expiresAt);
                        tokenRepository.save(confirmToken);

                        // Preparar contexto para el template
                        Context context = new Context();
                        context.setVariable("customerName",
                                        transaction.getCreditCard().getCustomer().getFirstName());
                        context.setVariable("cardNumber",
                                        transaction.getCreditCard().getMaskedCardNumber());
                        context.setVariable("amount",
                                        String.format("$%.2f", transaction.getAmt()));
                        context.setVariable("merchant",
                                        transaction.getMerchant());
                        context.setVariable("transactionDate",
                                        transaction.getTransDateTime()
                                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        context.setVariable("confirmUrl",
                                        confirmationBaseUrl + "/confirm/" + token);
                        context.setVariable("blockUrl",
                                        confirmationBaseUrl + "/block/" + token);
                        context.setVariable("expiryHours",
                                        tokenExpiryHours);

                        // Delegar envío al servicio genérico
                        String subject = "⚠️ Actividad sospechosa detectada en tu tarjeta " +
                                        transaction.getCreditCard().getMaskedCardNumber();

                        emailService.sendTemplatedEmail(
                                        customerEmail,
                                        subject,
                                        "email/fraud-alert-email",
                                        context);

                        log.info("✅ Email de alerta enviado a: {} para transacción: {}",
                                        customerEmail, transaction.getTransNum());

                } catch (Exception e) {
                        log.error("❌ Error al enviar email de alerta de fraude: {}", e.getMessage(), e);
                        throw new RuntimeException("Error al enviar email de alerta", e);
                }
        }

        /**
         * Generar token UUID seguro
         */
        public String generateSecureToken() {
                return UUID.randomUUID().toString();
        }
}
