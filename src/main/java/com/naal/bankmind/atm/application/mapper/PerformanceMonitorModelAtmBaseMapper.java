package com.naal.bankmind.atm.application.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

import com.naal.bankmind.atm.application.dto.response.PSIFeatureResultDTO;
import com.naal.bankmind.atm.application.dto.response.PerformanceMonitorModelAtmBaseDTO;
import com.naal.bankmind.atm.domain.model.PSIFeatureResultModel;
import com.naal.bankmind.atm.domain.model.PerformanceMonitorModelAtmBase;

public class PerformanceMonitorModelAtmBaseMapper {

    public static PerformanceMonitorModelAtmBaseDTO toPerformanceMonitorModelAtmBaseDTO(PerformanceMonitorModelAtmBase performanceMonitorModelAtm) {
        return new PerformanceMonitorModelAtmBaseDTO(
            performanceMonitorModelAtm.id(),
            toPSIFeatureResultMap(performanceMonitorModelAtm.psiResults()),
            performanceMonitorModelAtm.mae(),
            performanceMonitorModelAtm.rmse(),
            performanceMonitorModelAtm.mape(),
            performanceMonitorModelAtm.decision().name(),
            performanceMonitorModelAtm.message(),
            performanceMonitorModelAtm.action(),
            performanceMonitorModelAtm.summary(),
            performanceMonitorModelAtm.detail(),
            performanceMonitorModelAtm.createdAt(),
            performanceMonitorModelAtm.needSelfTraining()
        );
    }

    private static PSIFeatureResultDTO toPSIFeatureResultDTO(PSIFeatureResultModel psIFeatureResult) {
        return new PSIFeatureResultDTO(
            psIFeatureResult.getPsi(),
            psIFeatureResult.getAlert(),
            toBigDecimalArrayRedondeado(psIFeatureResult.getActualPct()),
            toBigDecimalArrayRedondeado(psIFeatureResult.getExpectedPct()),
            psIFeatureResult.getProdSamples(),
            psIFeatureResult.getProdNullPct()
        );
    }

    private static BigDecimal[] toBigDecimalArrayRedondeado(BigDecimal[] array) {
        if (array == null) {
            return null;
        }

        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].setScale(2, RoundingMode.HALF_UP);
        }
        return array;
    }

    private static Map<String, PSIFeatureResultDTO> toPSIFeatureResultMap(Map<String, ? extends PSIFeatureResultModel> psIFeatureResultMap) {
        if (psIFeatureResultMap == null) {
            return null;
        }
        return psIFeatureResultMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> toPSIFeatureResultDTO(entry.getValue())
            ));
    }
}
