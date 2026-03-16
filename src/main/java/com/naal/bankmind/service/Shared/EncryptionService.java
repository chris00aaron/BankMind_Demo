package com.naal.bankmind.service.Shared;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio de encriptación AES-256-GCM para datos sensibles.
 * Usa GCM (Galois/Counter Mode) que provee tanto confidencialidad como
 * integridad.
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes (recomendado para GCM)

    @Value("${bankmind.encryption.secret-key}")
    private String secretKey;

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        // La clave debe tener exactamente 32 bytes para AES-256
        byte[] keyBytes = secretKey.getBytes();
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "La clave de encriptación debe tener exactamente 32 caracteres (256 bits). " +
                            "Longitud actual: " + keyBytes.length);
        }
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        log.info("🔐 EncryptionService inicializado correctamente con AES-256-GCM");
    }

    /**
     * Encripta un texto plano y retorna el resultado en Base64.
     * El IV se genera aleatoriamente y se prepende al ciphertext.
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            // Generar IV aleatorio
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Combinar IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(IV_LENGTH + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Error al encriptar: {}", e.getMessage());
            throw new RuntimeException("Error al encriptar datos", e);
        }
    }

    /**
     * Desencripta un texto cifrado en Base64 y retorna el texto plano.
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            // Extraer IV y ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            log.error("Error al desencriptar: {}", e.getMessage());
            throw new RuntimeException("Error al desencriptar datos", e);
        }
    }
}
