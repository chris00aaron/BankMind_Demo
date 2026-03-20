package com.naal.bankmind.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Servicio GENÉRICO para envío de emails
 * Reutilizable por todos los módulos: Fraude, Marketing, Notificaciones, etc.
 * 
 * Responsabilidad: Enviar emails con templates HTML o texto plano
 * Principio SOLID: Single Responsibility - Solo se encarga de enviar emails
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${bankmind.email.from}")
    private String defaultEmailFrom;

    @Value("${bankmind.email.from-name}")
    private String defaultEmailFromName;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Enviar email usando template HTML de Thymeleaf
     * 
     * @param to           Email del destinatario
     * @param subject      Asunto del email
     * @param templateName Nombre del template (ej: "email/fraud-alert-email")
     * @param context      Contexto con variables para el template
     */
    public void sendTemplatedEmail(String to, String subject, String templateName, Context context) {
        try {
            // Renderizar template HTML
            String htmlContent = templateEngine.process(templateName, context);

            // Enviar con logo inline
            sendHtmlEmail(to, subject, htmlContent, defaultEmailFrom, defaultEmailFromName);

            log.info("✅ Email templated enviado exitosamente a: {} (Template: {})", to, templateName);

        } catch (Exception e) {
            log.error("❌ Error al enviar email templated a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error al enviar email templated", e);
        }
    }

    /**
     * Enviar email HTML con remitente personalizado
     * Útil para módulos que necesitan remitente diferente (ej:
     * marketing@bankmind.com)
     */
    public void sendTemplatedEmail(String to, String subject, String templateName, Context context,
            String fromEmail, String fromName) {
        try {
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmail(to, subject, htmlContent, fromEmail, fromName);

            log.info("✅ Email templated enviado a: {} desde: {} ({})", to, fromName, fromEmail);

        } catch (Exception e) {
            log.error("❌ Error al enviar email templated: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar email templated", e);
        }
    }

    /**
     * Enviar email HTML directo (sin template)
     * Útil para contenido generado dinámicamente
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendHtmlEmail(to, subject, htmlContent, defaultEmailFrom, defaultEmailFromName);
    }

    /**
     * Enviar email HTML con todos los parámetros personalizados
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent, String fromEmail, String fromName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);

            log.info("✅ Email HTML enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Error de mensajería al enviar email a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error al enviar email HTML", e);
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar email a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error al procesar email", e);
        }
    }

    /**
     * Enviar email de texto plano (sin HTML)
     * Útil para notificaciones simples o sistemas legacy
     */
    public void sendPlainTextEmail(String to, String subject, String textContent) {
        sendPlainTextEmail(to, subject, textContent, defaultEmailFrom, defaultEmailFromName);
    }

    /**
     * Enviar email de texto plano con remitente personalizado
     */
    public void sendPlainTextEmail(String to, String subject, String textContent, String fromEmail, String fromName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, false); // false = plain text

            mailSender.send(message);

            log.info("✅ Email de texto plano enviado a: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Error al enviar email de texto plano a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error al enviar email de texto plano", e);
        } catch (Exception e) {
            log.error("❌ Error inesperado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar email", e);
        }
    }

    /**
     * Validar que un email no esté vacío
     * Método helper para validaciones comunes
     */
    public boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }
}
