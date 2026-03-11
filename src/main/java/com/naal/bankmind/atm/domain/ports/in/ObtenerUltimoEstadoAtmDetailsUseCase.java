package com.naal.bankmind.atm.domain.ports.in;

import java.util.List;

import com.naal.bankmind.atm.application.dto.response.UltimoEstadoAtmDetailsUseDTO;

public interface ObtenerUltimoEstadoAtmDetailsUseCase {
    
    /**
     * Obtiene el último estado de todos los cajeros activos
     * Muestra el detalle de cada cajero (ultimo estado, saldo actual, etc)
     * @return Lista de últimos estados de los cajeros activos
     */
    List<UltimoEstadoAtmDetailsUseDTO> obtenerUltimoEstadoAtmDetails();
}
