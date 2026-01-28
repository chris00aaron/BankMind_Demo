package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con datos mínimos que envía el Punto de Venta (POS) o App
 * El backend enriquecerá estos datos con información del cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTransactionRequestDto {

    /**
     * Número de tarjeta de crédito (identifica al cliente)
     */
    @NotNull(message = "El número de tarjeta es obligatorio")
    @JsonProperty("cc_num")
    private Long ccNum;

    /**
     * Monto de la transacción
     */
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    @JsonProperty("amt")
    private Double amt;

    /**
     * Nombre del comercio
     */
    @JsonProperty("merchant")
    private String merchant;

    /**
     * Categoría del comercio (shopping_net, grocery_pos, etc.)
     */
    @NotNull(message = "La categoría es obligatoria")
    @JsonProperty("category")
    private String category;

    /**
     * Latitud del comercio
     */
    @JsonProperty("merch_lat")
    private Double merchLat;

    /**
     * Longitud del comercio
     */
    @JsonProperty("merch_long")
    private Double merchLong;
}
