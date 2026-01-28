package com.naal.bankmind.dto.Default.Request;

import lombok.Data;

/**
 * DTO para filtrar clientes en predicción por lotes.
 */
@Data
public class BatchFilterRequestDTO {

    private Integer edadMin;
    private Integer edadMax;
    private String educacion; // "all" o valor específico
    private String estadoCivil; // "all" o valor específico
    private String fechaDesde; // formato "yyyy-MM-dd"
    private String fechaHasta; // formato "yyyy-MM-dd"
}
