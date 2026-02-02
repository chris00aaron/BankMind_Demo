package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Response.DashboardMorosidadDTO;
import com.naal.bankmind.service.Default.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para el dashboard de morosidad.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Obtiene todos los datos del dashboard de morosidad.
     */
    @GetMapping("/morosidad")
    public ResponseEntity<DashboardMorosidadDTO> getDashboardMorosidad() {
        DashboardMorosidadDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }
}
