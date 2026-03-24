package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.PromedioRetiroHistorico;
import com.naal.bankmind.atm.domain.ports.out.repository.PromedioRetiroHistoricoRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmRepository;

import lombok.AllArgsConstructor;


@AllArgsConstructor
@Repository
public class PromedioRetiroHistoricoDbAdapter implements PromedioRetiroHistoricoRepository {

    private final JpaAtmRepository jpaAtmRepository;

    @Override
    public List<PromedioRetiroHistorico> obtenerPromediosHistoricos(Short dia, Short mes) {
        return jpaAtmRepository.obtenerRetiroDePromedioHistorico(dia, mes)
            .stream()
            .map(projection -> new PromedioRetiroHistorico(projection.getIdAtm(), projection.getAvgWithdrawal()))
            .toList();
    }
}
