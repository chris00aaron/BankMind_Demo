package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.PerformanceMonitorModelAtmBaseDTO;
import com.naal.bankmind.atm.domain.ports.in.ObtenerUltimoRegistroMonitoreoUseCase;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;


@AllArgsConstructor
@RestController
@RequestMapping("/api/atm/monitoring")
public class PerformanceMonitorModelAtmController {

    private final ObtenerUltimoRegistroMonitoreoUseCase obtenerUltimoRegistroMonitoreoUseCase;

    @GetMapping("/last")
    public ResponseEntity<ApiResponse<PerformanceMonitorModelAtmBaseDTO>> getLastMonitoring() {
        var monitoring = obtenerUltimoRegistroMonitoreoUseCase.getLastMonitorin();
        return ResponseEntity.ok(ApiResponse.success(
            "Se encontro el ultimo registro de monitoreo del "+ monitoring.createdAt() 
            ,monitoring));
    }
    
}
