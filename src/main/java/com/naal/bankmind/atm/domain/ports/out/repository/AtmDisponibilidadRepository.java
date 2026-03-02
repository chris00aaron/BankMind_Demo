package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.List;

import com.naal.bankmind.atm.domain.model.AtmDisponibilidad;

public interface AtmDisponibilidadRepository {

    /**
     * Obtiene la disponibilidad actual de los cajeros
     * @return Lista de AtmDisponibilidad
     */
    List<AtmDisponibilidad> obtenerDisponibilidadActual();

}
