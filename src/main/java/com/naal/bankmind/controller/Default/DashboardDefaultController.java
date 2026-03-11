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
@RequestMapping("/api/morosidad/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardDefaultController {

    private final DashboardService dashboardService;

    /**
     * Obtiene todos los datos del dashboard de morosidad.
     */
    @GetMapping("/morosidad")
    public ResponseEntity<DashboardMorosidadDTO> getDashboardMorosidad() {
        DashboardMorosidadDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Obtiene los clientes con paginación y filtros.
     */
    @GetMapping("/clientes")
    public ResponseEntity<org.springframework.data.domain.Page<com.naal.bankmind.dto.Default.Response.DashboardMorosidadDTO.ClienteAltoRiesgo>> getDashboardClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String clasificacionSBS,
            @RequestParam(defaultValue = "probabilidadPago") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String educacion,
            @RequestParam(required = false) Integer edadMin,
            @RequestParam(required = false) Integer edadMax) {

        var result = dashboardService.getDashboardClientsPaginated(
                page, size, nombre, clasificacionSBS, sortBy, sortDir, educacion, edadMin, edadMax);
        return ResponseEntity.ok(result);
    }
}
