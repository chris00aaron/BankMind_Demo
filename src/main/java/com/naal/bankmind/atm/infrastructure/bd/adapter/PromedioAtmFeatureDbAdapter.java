package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.PromedioAtmFeature;
import com.naal.bankmind.atm.domain.ports.out.repository.PromedioAtmFeatureRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmRepository;
import com.naal.bankmind.atm.infrastructure.bd.projections.AtmAvgProjection;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class PromedioAtmFeatureDbAdapter implements PromedioAtmFeatureRepository {

    private final JpaAtmRepository jpaAtmRepository;

    @Override
    public List<PromedioAtmFeature> obtenerPromediosAtmFeatures(Short diaDelMes, Short mesSolicitado) {
        return jpaAtmRepository.obtenerPromediosAtmFeatures(diaDelMes, mesSolicitado)
                .stream()
                .map(this::toPromedioAtmFeature)
                .toList();
    }

    private PromedioAtmFeature toPromedioAtmFeature(AtmAvgProjection projection) {
        return new PromedioAtmFeature(
                projection.getIdAtm(),
                projection.getLocationType(),
                projection.getAvgLag1(),
                projection.getAvgLag5(),
                projection.getAvgLag11(),
                projection.getAvgTendenciaLags(),
                projection.getAvgRatioFindeVsSemana(),
                projection.getAvgRetirosFindeAnterior(),
                projection.getAvgRetirosDomingoAnterior(),
                projection.getAvgDomingoBajo(),
                projection.getAvgCaidaReciente());
    }
}
