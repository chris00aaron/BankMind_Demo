package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.dto.atm.response.ModelProductionDTO;
import com.naal.bankmind.service.atm.WithdrawalModelService;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.GET, RequestMethod.POST}
)
@RestController
@RequestMapping("/atm/model")
public class WithdrawalModelController {

    private final WithdrawalModelService withdrawalModelService;

    @GetMapping("/production")
    public ResponseEntity<ApiResponse<ModelProductionDTO>> modeloEnProduccion() {
        return ResponseEntity.ok(ApiResponse.success("Modelo en produccion",withdrawalModelService.obtenerModeloEnProduccion()));
    }
}
