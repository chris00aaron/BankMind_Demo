package com.naal.bankmind.controller.atm;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.dto.atm.response.ResumeOperativoDTO;
import com.naal.bankmind.dto.atm.response.RetiroEfectivoAtmPrediccionDTO;
import com.naal.bankmind.dto.atm.response.RetiroEfectivoAtmPrediccionResumenDTO;
import com.naal.bankmind.dto.atm.response.RetiroHistoricoDTO;
import com.naal.bankmind.dto.atm.response.SegmentacionRetiroDTO;
import com.naal.bankmind.mapper.atm.DailyWithdrawalPredictionMapper;
import com.naal.bankmind.service.atm.AtmFeaturesService;
import com.naal.bankmind.service.atm.AtmService;
import com.naal.bankmind.service.atm.DailyWithdrawalPredictionService;
import com.naal.bankmind.utils.atm.ModelConfidenceService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.GET, RequestMethod.POST}
)
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DailyWithdrawalPredictionService dailyWithdrawalPredictionService;
    private final AtmFeaturesService atmFeaturesService;
    private final AtmService atmService;
    private final ModelConfidenceService modelConfidenceService;

    @GetMapping
    public ResponseEntity<?> obtenerRetirosPorFecha() {
        LocalDate fecha = LocalDate.of(2025, 12, 2);
        var predictionBrutas = dailyWithdrawalPredictionService.obtenerPrediccionesPorFecha(fecha);

        var predicciones = predictionBrutas.stream().map(DailyWithdrawalPredictionMapper::toRetiroEfectivoAtmPrediccionDTO).toList();
        var resumen = RetiroEfectivoAtmPrediccionResumenDTO.from(predicciones);
        var resumenOperativo = atmService.obtenerResumenOperatividad();
        var retirosHistoricos = atmFeaturesService.predecirBasadoEnHistoricoComparadoConPrediccion((short) fecha.getDayOfMonth(), (short) fecha.getMonthValue(), predicciones);
        var atmsConPotencialDeFaltaStock = atmService.contabilizarAtmsConPotencialDeFaltaStock(fecha, predicciones);
        var segmentacionRetiro = SegmentacionRetiroDTO.from(predictionBrutas);
        var fectures = modelConfidenceService.mostrarImportanciaFeatures();
        return ResponseEntity.ok(new dtoDashboard(resumen,resumenOperativo,atmsConPotencialDeFaltaStock,predicciones,retirosHistoricos, fectures, segmentacionRetiro));
    }
    
    public record dtoDashboard(
        RetiroEfectivoAtmPrediccionResumenDTO resumenRetiroEfectivoAtm,
        ResumeOperativoDTO resumenOperativoAtms,
        Long atmsConPotencialDeFaltaStock,
        List<RetiroEfectivoAtmPrediccionDTO> retirosPredichos,
        List<RetiroHistoricoDTO> retirosHistoricos,
        Map<String, Object> featuresImportancia,
        SegmentacionRetiroDTO segmentacionRetiro
    ) {}
}
