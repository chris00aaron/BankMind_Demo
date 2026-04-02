package com.naal.bankmind.controller.atm;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.WeatherDTO;
import com.naal.bankmind.atm.domain.ports.in.ListarClimasDisponiblesUseCase;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/atm/weather")
public class WeatherController {

    private final ListarClimasDisponiblesUseCase listarClimasDisponiblesUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WeatherDTO>>> obtenerTiposDeClima() {
        return ResponseEntity.ok(ApiResponse.success("Tipos de clima obtenidos correctamente", listarClimasDisponiblesUseCase.listarClimasDisponibles()));
    }
}
