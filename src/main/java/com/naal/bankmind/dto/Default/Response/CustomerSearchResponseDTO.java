package com.naal.bankmind.dto.Default.Response;

import java.util.List;

/**
 * DTO para respuesta de búsqueda de clientes.
 */
public record CustomerSearchResponseDTO(
                Long idCustomer,
                String nombre,
                Integer edad,
                String educacion,
                String estadoCivil,
                String fechaRegistro,
                List<AccountSummaryDTO> cuentas) {
}
