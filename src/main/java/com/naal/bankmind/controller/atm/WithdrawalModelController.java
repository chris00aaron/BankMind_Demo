package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.ModelProductionDTO;
import com.naal.bankmind.atm.domain.ports.in.ObtenerModeloActualEnProduccionUseCase;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@RestController
@RequestMapping("/api/atm/model")
public class WithdrawalModelController {

    private final ObtenerModeloActualEnProduccionUseCase obtenerModeloActualEnProduccionUseCase;

    @GetMapping("/production")
    public ResponseEntity<ApiResponse<ModelProductionDTO>> modeloEnProduccion() {
        ModelProductionDTO modelProductionDTO = obtenerModeloActualEnProduccionUseCase.getModeloActualEnProduccion();
        return ResponseEntity.ok(ApiResponse.success("Modelo en produccion", modelProductionDTO));
    }
}