package com.naal.bankmind.atm.infrastructure.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.entity.atm.AtmFeatures;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class DailyAtmTransactionMapper {

    public static InputDataPredictionRetiroAtm toInputDataPredictionRetiroAtm(DailyAtmTransaction transaction) {
        if (transaction == null) return null;

        InputDataPredictionRetiroAtm dto = new InputDataPredictionRetiroAtm();
        
        // Navegación segura
        if (transaction.getAtm() != null) {
            dto.setAtm(transaction.getAtm().getIdAtm());
            // Cuidado: Si locationType es LAZY, esto disparará otra consulta SQL
            if (transaction.getAtm().getLocationType() != null) {
                dto.setUbicacion(transaction.getAtm().getLocationType().getIdLocationType());
            }
        }
        
        if (transaction.getWeather() != null) {
            dto.setAmbiente(transaction.getWeather().getImpact());
        }

        // Obtener la feature más reciente de forma segura
        // Usamos orElse(null) para evitar el error del .get()
        AtmFeatures feature = transaction.getFeatures().stream()
                .max(Comparator.comparing(AtmFeatures::getCreatedAt)) 
                .orElse(null);
                
        dto.setDiaSemana(feature.getDayOfWeek());

        Map<String, Object> df = feature.getDynamicFeatures();
        if (df != null) {
            dto.setTendenciaLags(toBigDecimal(df.get("tendencia_lags")));
            dto.setLag1(toBigDecimal(df.get("lag1")));
            dto.setLag5(toBigDecimal(df.get("lag5")));
            dto.setLag11(toBigDecimal(df.get("lag11")));
            
            // Cast seguro usando pattern matching de Java 16+
            dto.setCaidaReciente(df.get("caida_reciente") instanceof Number n ? n.shortValue() : (short) 0);
            dto.setRetirosFindeAnterior(toBigDecimal(df.get("retiros_finde_anterior")));
            dto.setRetirosDomingoAnterior(toBigDecimal(df.get("retiros_domingo_anterior")));
            dto.setRatioFindeVsSemana(toBigDecimal(df.get("ratio_finde_vs_semana")));
            dto.setDomingoBajo(df.get("domingo_bajo") instanceof Number n ? n.shortValue() : (short) 0);
        }
        return dto;
    }

    private static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
