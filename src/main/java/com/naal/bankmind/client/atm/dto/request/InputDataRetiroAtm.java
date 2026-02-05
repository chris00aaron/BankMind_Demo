package com.naal.bankmind.client.atm.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class InputDataRetiroAtm {

    private Long atm;
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
}
