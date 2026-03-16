package com.naal.bankmind.config.Login;

import com.naal.bankmind.service.Shared.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter que encripta/desencripta automáticamente
 * los campos de entidad marcados con @Convert(converter =
 * AttributeEncryptor.class).
 *
 * La encriptación es transparente para la aplicación:
 * - Al guardar en BD → encripta
 * - Al leer de BD → desencripta
 */
@Converter
@Component
@RequiredArgsConstructor
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
