package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.dto.atm.request.RetiroEfectivoAtmBasadoEnHistoricoRequestDTO;
import com.naal.bankmind.dto.atm.response.PrediccionDeRetirosDTO;
import com.naal.bankmind.service.atm.AtmFeaturesService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.POST}
)
@RestController
@RequestMapping("/atm/simulador")
public class SimuladorController {

    private final AtmFeaturesService atmFeaturesService;

    @PostMapping("/retiro-efectivo-atm/historico")
    public ResponseEntity<PrediccionDeRetirosDTO> simularRetiroEfectivoAtm(
        @Valid @RequestBody RetiroEfectivoAtmBasadoEnHistoricoRequestDTO requestDTO
    ) {
        log.info("Simulando retiro de efectivo en ATM, {}", requestDTO);
        return ResponseEntity.ok(atmFeaturesService.predecirBasadoEnHistorico(requestDTO));
    }
}
