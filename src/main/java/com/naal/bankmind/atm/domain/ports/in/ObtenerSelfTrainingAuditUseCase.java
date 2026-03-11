package com.naal.bankmind.atm.domain.ports.in;

import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.atm.domain.model.PageResult;

public interface ObtenerSelfTrainingAuditUseCase {

    /**
     * Obtiene el historial paginado de registros de autoentrenamiento del modelo de
     * retiros.
     * 
     * @param page número de página (0-based)
     * @param size tamaño de la página
     * @return página de registros de auditoría de autoentrenamiento
     */
    PageResult<RegistroAutoentrenamientoDTO> obtenerHistorialAutoentrenamiento(int page, int size);

    /**
     * Obtiene el detalle completo de un registro de autoentrenamiento por su ID.
     * 
     * @param id identificador único del registro
     * @return detalles del registro de autoentrenamiento
     */
    RegistroAutoentrenamientoDetailsDTO obtenerDetallePorId(Long id);
}
