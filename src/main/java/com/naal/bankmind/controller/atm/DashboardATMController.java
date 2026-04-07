package com.naal.bankmind.controller.atm;


import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.usecase.DashboardOrchestrator;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@RestController
@RequestMapping("/api/atm/dashboard")
public class DashboardATMController {
    private final DashboardOrchestrator dashboardOrchestrator;

    @GetMapping
    public ResponseEntity<?> obtenerRetirosPorFecha() {
        var dashboard = dashboardOrchestrator.generarDashboard();
        LocalDate fecha = LocalDate.of(2025, 12, 2);
        String mensaje = String.format("Dashboard generado para fecha: %s", fecha);

        return ResponseEntity.ok(ApiResponse.success(mensaje, dashboard));
    }
}