package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Response.PortfolioResponseDTO;
import com.naal.bankmind.service.Default.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para la gestión de cartera en riesgo.
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Obtiene las cuentas de la cartera con filtros opcionales.
     *
     * @param nivelRiesgo      Niveles de riesgo separados por coma
     *                         (Crítico,Alto,Medio,Bajo)
     * @param clasificacionSBS Categorías SBS separadas por coma
     *                         (Normal,CPP,Deficiente,Dudoso,Pérdida)
     * @param probMin          Probabilidad de pago mínima (0-100)
     * @param probMax          Probabilidad de pago máxima (0-100)
     */
    @GetMapping("/risk-accounts")
    public ResponseEntity<PortfolioResponseDTO> getRiskAccounts(
            @RequestParam(required = false) List<String> nivelRiesgo,
            @RequestParam(required = false) List<String> clasificacionSBS,
            @RequestParam(required = false) Double probMin,
            @RequestParam(required = false) Double probMax) {
        PortfolioResponseDTO response = portfolioService.getRiskAccounts(
                nivelRiesgo, clasificacionSBS, probMin, probMax);
        return ResponseEntity.ok(response);
    }
}
