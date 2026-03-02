package com.naal.bankmind.controller.atm;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.EstadoAtmDTO;
import com.naal.bankmind.atm.domain.ports.in.ObtenerEstadoActualAtmUseCase;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true", methods = {
        RequestMethod.GET, RequestMethod.POST })
@RestController
@RequestMapping("/atm/status")
public class AtmController {

    private final ObtenerEstadoActualAtmUseCase obtenerEstadoActualAtmUseCase;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<EstadoAtmDTO>>> getAllDailyWithdrawalPredictions() {
        return ResponseEntity.ok(ApiResponse.success("Estados de cajeros obtenidos correctamente",
                obtenerEstadoActualAtmUseCase.obtenerEstadoActualAtm()));
    }
}
