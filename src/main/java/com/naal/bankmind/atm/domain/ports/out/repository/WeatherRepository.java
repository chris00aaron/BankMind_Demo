package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.List;
import java.util.Optional;

import com.naal.bankmind.atm.domain.model.Weather;

public interface WeatherRepository {

    /**
     * Obtiene todos los climas disponibles
     * @return Lista de climas disponibles
     */
    List<Weather> findAll();

    /**
     * Obtiene el clima por ID
     * @param idWeather ID del clima
     * @return Clima
     */
    Optional<Weather> findById(Short idWeather);
}
