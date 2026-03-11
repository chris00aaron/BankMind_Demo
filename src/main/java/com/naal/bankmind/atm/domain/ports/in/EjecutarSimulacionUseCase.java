package com.naal.bankmind.atm.domain.ports.in;

import java.time.LocalDate;

import com.naal.bankmind.atm.application.dto.response.PrediccionDeRetirosDTO;

public interface EjecutarSimulacionUseCase {

    PrediccionDeRetirosDTO ejecutarSimulacion(LocalDate fecha, Short idWeather);
}
