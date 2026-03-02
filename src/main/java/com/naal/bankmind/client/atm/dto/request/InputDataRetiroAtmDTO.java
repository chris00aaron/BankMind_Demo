package com.naal.bankmind.client.atm.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import lombok.Data;

@Data
public class InputDataRetiroAtmDTO {

    private Long atm;
    private LocalDate prediction_date;
    private Short diaSemana;
    private BigDecimal tendencia_lags;
    private BigDecimal lag1;
    private BigDecimal lag5;
    private BigDecimal lag11;
    private Short caida_reciente;
    private BigDecimal retiros_finde_anterior;
    private BigDecimal retiros_domingo_anterior;
    private BigDecimal ratio_finde_vs_semana;
    private Short domingo_bajo;
    private Integer ubicacion;
    private Short ambiente;

    public InputDataRetiroAtmDTO (InputDataPredictionRetiroAtm inputData) {
        this.atm = inputData.getAtm();
        this.prediction_date = inputData.getFechaPrediccion();
        this.diaSemana = inputData.getDiaSemana();
        this.tendencia_lags = inputData.getTendenciaLags();
        this.lag1 = inputData.getLag1();
        this.lag5 = inputData.getLag5();
        this.lag11 = inputData.getLag11();
        this.caida_reciente = inputData.getCaidaReciente();
        this.retiros_finde_anterior = inputData.getRetirosFindeAnterior();
        this.retiros_domingo_anterior = inputData.getRetirosDomingoAnterior();
        this.ratio_finde_vs_semana = inputData.getRatioFindeVsSemana();
        this.domingo_bajo = inputData.getDomingoBajo();
        this.ubicacion = inputData.getUbicacion();
        this.ambiente = inputData.getAmbiente();
    }
}
