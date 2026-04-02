package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.request.RetiroEfectivoAtmBasadoEnHistoricoRequestDTO;
import com.naal.bankmind.atm.application.dto.response.PrediccionDeRetirosDTO;
import com.naal.bankmind.atm.domain.ports.in.EjecutarSimulacionUseCase;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor

@RestController
@RequestMapping("/atm/simulador")
public class SimuladorController {

    private final EjecutarSimulacionUseCase ejecutarSimulacionUseCase;

    @PostMapping("/retiro-efectivo-atm/historico")
    public ResponseEntity<PrediccionDeRetirosDTO> simularRetiroEfectivoAtm(
        @Valid @RequestBody RetiroEfectivoAtmBasadoEnHistoricoRequestDTO requestDTO
    ) {
        log.info("Simulando retiro de efectivo en ATM, {}", requestDTO);
        PrediccionDeRetirosDTO result = ejecutarSimulacionUseCase.ejecutarSimulacion(requestDTO.fechaObjetivo(), requestDTO.idWeather());
        log.info("Resultado de la simulación: {}", result);
        return ResponseEntity.ok(result);
    }
}
