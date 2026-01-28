package com.naal.bankmind.entity.Default.POJO;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DetalleColumna implements Serializable {

    private String name;

    @JsonProperty("date_type")
    private String dateType;

    private String rol;

    private String description;

    @JsonProperty("is_nullable")
    private Boolean isNullable = true;

    /*
     * EJEMPLO DE JSON
     * [
     * {
     * "nombre": "ID_CLIENTE",
     * "tipo_dato": "INTEGER",
     * "rol": "ID",
     * "descripcion": "Identificador único (No usado para predecir)"
     * },
     * {
     * "nombre": "LIMIT_BAL",
     * "tipo_dato": "FLOAT",
     * "rol": "FEATURE",
     * "descripcion": "Monto del crédito otorgado"
     * },
     * {
     * "nombre": "PAY_0",
     * "tipo_dato": "INTEGER",
     * "rol": "FEATURE",
     * "descripcion": "Estado de pago mes actual (-1=Pago a tiempo, 1=Retraso)"
     * },
     * {
     * "nombre": "EDAD",
     * "tipo_dato": "INTEGER",
     * "rol": "FEATURE",
     * "descripcion": "Edad del cliente en años"
     * },
     * {
     * "nombre": "default_payment_next_month",
     * "tipo_dato": "INTEGER",
     * "rol": "TARGET",
     * "descripcion": "Variable Objetivo (1=Moroso, 0=No Moroso)"
     * }
     * ]
     */

}
