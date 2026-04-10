package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.dto.Fraud.WhatIfRequestDto;
import com.naal.bankmind.dto.Fraud.WhatIfResponseDto;
import com.naal.bankmind.service.Fraud.WhatIfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para predicciones What-If (simulación)
 * 
 * Estas predicciones NO se guardan en la base de datos.
 * Son útiles para que los analistas prueben escenarios hipotéticos.
 */
@RestController
@RequestMapping("/api/fraud/what-if")
public class WhatIfController {

    private final WhatIfService whatIfService;

    public WhatIfController(WhatIfService whatIfService) {
        this.whatIfService = whatIfService;
    }

    /**
     * POST /api/fraud/what-if/simulate
     * Simula una predicción de fraude SIN guardarla en BD
     */
    @PostMapping("/simulate")
    public ResponseEntity<WhatIfResponseDto> simulatePrediction(@RequestBody WhatIfRequestDto request) {
        WhatIfResponseDto response = whatIfService.simulatePrediction(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/fraud/what-if/customer/{ccNum}
     * Busca información del cliente por número de tarjeta (para mostrar antes de
     * simular)
     */
    @GetMapping("/customer/{ccNum}")
    public ResponseEntity<WhatIfResponseDto> lookupCustomer(@PathVariable Long ccNum) {
        WhatIfResponseDto response = whatIfService.lookupCustomer(ccNum);
        return ResponseEntity.ok(response);
    }
}
