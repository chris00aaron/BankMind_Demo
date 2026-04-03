package com.naal.bankmind.controller.atm;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.EstadoAtmDTO;
import com.naal.bankmind.atm.application.dto.response.UltimoEstadoAtmDetailsUseDTO;
import com.naal.bankmind.atm.domain.ports.in.ObtenerEstadoActualAtmUseCase;
import com.naal.bankmind.atm.domain.ports.in.ObtenerUltimoEstadoAtmDetailsUseCase;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/api/atm/status")
public class AtmController {

    private final ObtenerEstadoActualAtmUseCase obtenerEstadoActualAtmUseCase;
    private final ObtenerUltimoEstadoAtmDetailsUseCase obtenerUltimoEstadoAtmDetailsUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EstadoAtmDTO>>> getAllDailyWithdrawalPredictions() {
        return ResponseEntity.ok(ApiResponse.success("Estados de cajeros obtenidos correctamente",
                obtenerEstadoActualAtmUseCase.obtenerEstadoActualAtm()));
    }

    @GetMapping("/last")
    public ResponseEntity<ApiResponse<List<UltimoEstadoAtmDetailsUseDTO>>> getUltimoEstadoAtmDetails() {
        return ResponseEntity.ok(ApiResponse.success("Estados de cajeros obtenidos correctamente",
                obtenerUltimoEstadoAtmDetailsUseCase.obtenerUltimoEstadoAtmDetails()));
    }
}