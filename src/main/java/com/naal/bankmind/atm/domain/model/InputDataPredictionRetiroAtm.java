package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InputDataPredictionRetiroAtm {
    private Long atm;
    private LocalDate fechaPrediccion;
    private Short diaSemana;
    private BigDecimal tendenciaLags;
    private BigDecimal lag1;
    private BigDecimal lag5;
    private BigDecimal lag11;
    private Short caidaReciente;
    private BigDecimal retirosFindeAnterior;
    private BigDecimal retirosDomingoAnterior;
    private BigDecimal ratioFindeVsSemana;
    private Short domingoBajo;
    private Integer ubicacion;
    private Short ambiente;

    public Long getAtm() {
        return atm;
    }
    public void setAtm(Long atm) {
        this.atm = atm;
    }
    public LocalDate getFechaPrediccion() {
        return fechaPrediccion;
    }
    public void setFechaPrediccion(LocalDate fechaPrediccion) {
        this.fechaPrediccion = fechaPrediccion;
    }
    public Short getDiaSemana() {
        return diaSemana;
    }
    public void setDiaSemana(Short diaSemana) {
        this.diaSemana = diaSemana;
    }
    public BigDecimal getTendenciaLags() {
        return tendenciaLags;
    }
    public void setTendenciaLags(BigDecimal tendenciaLags) {
        this.tendenciaLags = tendenciaLags;
    }
    public BigDecimal getLag1() {
        return lag1;
    }
    public void setLag1(BigDecimal lag1) {
        this.lag1 = lag1;
    }
    public BigDecimal getLag5() {
        return lag5;
    }
    public void setLag5(BigDecimal lag5) {
        this.lag5 = lag5;
    }
    public BigDecimal getLag11() {
        return lag11;
    }
    public void setLag11(BigDecimal lag11) {
        this.lag11 = lag11;
    }
    public Short getCaidaReciente() {
        return caidaReciente;
    }
    public void setCaidaReciente(Short caidaReciente) {
        this.caidaReciente = caidaReciente;
    }
    public BigDecimal getRetirosFindeAnterior() {
        return retirosFindeAnterior;
    }
    public void setRetirosFindeAnterior(BigDecimal retirosFindeAnterior) {
        this.retirosFindeAnterior = retirosFindeAnterior;
    }
    public BigDecimal getRetirosDomingoAnterior() {
        return retirosDomingoAnterior;
    }
    public void setRetirosDomingoAnterior(BigDecimal retirosDomingoAnterior) {
        this.retirosDomingoAnterior = retirosDomingoAnterior;
    }
    public BigDecimal getRatioFindeVsSemana() {
        return ratioFindeVsSemana;
    }
    public void setRatioFindeVsSemana(BigDecimal ratioFindeVsSemana) {
        this.ratioFindeVsSemana = ratioFindeVsSemana;
    }
    public Short getDomingoBajo() {
        return domingoBajo;
    }
    public void setDomingoBajo(Short domingoBajo) {
        this.domingoBajo = domingoBajo;
    }
    public Integer getUbicacion() {
        return ubicacion;
    }
    public void setUbicacion(Integer ubicacion) {
        this.ubicacion = ubicacion;
    }
    public Short getAmbiente() {
        return ambiente;
    }
    public void setAmbiente(Short ambiente) {
        this.ambiente = ambiente;
    }

    

}
