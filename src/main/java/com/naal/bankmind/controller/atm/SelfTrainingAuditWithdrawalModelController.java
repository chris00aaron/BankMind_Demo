package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.ports.in.ObtenerSelfTrainingAuditUseCase;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j

@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true", methods = {
        RequestMethod.GET, RequestMethod.POST })
@RestController
@RequestMapping("/atm/self-training")
public class SelfTrainingAuditWithdrawalModelController {

    private final ObtenerSelfTrainingAuditUseCase obtenerSelfTrainingAuditUseCase;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResult<RegistroAutoentrenamientoDTO>>> obtenerHistorialAutoentrenamiento(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Historial de auditorias de autoentrenamiento",
                obtenerSelfTrainingAuditUseCase.obtenerHistorialAutoentrenamiento(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RegistroAutoentrenamientoDetailsDTO>> obtenerDetalleAutoentrenamiento(
            @PathVariable Long id) {
        log.info("Solicitando detalle de autoentrenamiento con id: {}", id);
        RegistroAutoentrenamientoDetailsDTO detalle = obtenerSelfTrainingAuditUseCase.obtenerDetallePorId(id);
        return ResponseEntity.ok(ApiResponse.success("Detalle de registro de autoentrenamiento", detalle));
    }
}
