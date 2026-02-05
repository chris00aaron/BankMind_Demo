package com.naal.bankmind.controller.atm;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.dto.atm.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.dto.atm.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.service.atm.SelfTrainingAuditWithdrawalModelService;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.GET, RequestMethod.POST}
)
@RestController
@RequestMapping("/self-training")
public class SelfTrainingAuditWithdrawalModelController {

    private final SelfTrainingAuditWithdrawalModelService selfTrainingAuditWithdrawalModelService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<RegistroAutoentrenamientoDTO>>> obtenerModelosEnProduccion(
        @RequestParam(defaultValue = "0") int page, 
        @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.success("Historial de auditorias de autoentrenamiento",selfTrainingAuditWithdrawalModelService.obtenerModelosEnProduccion(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RegistroAutoentrenamientoDetailsDTO>> obtenerRegistroAutoentrenamiento(@PathVariable Long id) {
        RegistroAutoentrenamientoDetailsDTO registroAutoentrenamientoDetailsDTO = selfTrainingAuditWithdrawalModelService.obtenerModeloPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Registro de autoentrenamiento", registroAutoentrenamientoDetailsDTO));
    }
}
