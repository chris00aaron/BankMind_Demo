package com.naal.bankmind.atm.application.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.PromedioAtmFeature;

@Component
public class PromedioRetiroHistoricoMapper {

    public static InputDataPredictionRetiroAtm toInputDataRetiroAtm(PromedioAtmFeature p, LocalDate fechaSolicitada, Short impactoClimatico) {
         InputDataPredictionRetiroAtm inputData = new InputDataPredictionRetiroAtm();
         inputData.setAtm(p.idAtm());
         inputData.setFechaPrediccion(fechaSolicitada);
         inputData.setDiaSemana(getDiaSemana(fechaSolicitada));
         inputData.setTendenciaLags(p.avgTendenciaLags());
         inputData.setLag1(p.avgLag1());
         inputData.setLag5(p.avgLag5());
         inputData.setLag11(p.avgLag11());
         inputData.setCaidaReciente(p.avgCaidaReciente());
         inputData.setRetirosFindeAnterior(p.avgRetirosFindeAnterior());
         inputData.setRetirosDomingoAnterior(p.avgRetirosDomingoAnterior());
         inputData.setRatioFindeVsSemana(p.avgRatioFindeVsSemana());
         inputData.setDomingoBajo(p.avgDomingoBajo());
         inputData.setUbicacion(p.locationType());
         inputData.setAmbiente(impactoClimatico);
         return inputData;
    }

    private static Short getDiaSemana(LocalDate fecha) {
        return (short) fecha.getDayOfWeek().getValue();
    }
}
