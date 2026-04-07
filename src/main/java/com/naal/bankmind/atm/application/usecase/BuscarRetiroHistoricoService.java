package com.naal.bankmind.atm.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.RetiroHistoricoDTO;
import com.naal.bankmind.atm.domain.model.PromedioRetiroHistorico;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.atm.domain.ports.in.BuscarRetiroHistoricoUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.PromedioRetiroHistoricoRepository;

import lombok.AllArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
@Transactional(readOnly = true) 
public class BuscarRetiroHistoricoService implements BuscarRetiroHistoricoUseCase {

    private final PromedioRetiroHistoricoRepository promedioRetiroHistoricoRepository;

    @Override
    public List<RetiroHistoricoDTO> predecirBasadoEnHistoricoComparadoConPrediccion
        (Short diaDelMes, Short mes, List<RetiroEfectivoAtmPrediccion> prediccionDeRetiros) {
        
        List<PromedioRetiroHistorico> datosHistoricos = promedioRetiroHistoricoRepository.obtenerPromediosHistoricos(diaDelMes, mes);
        
        Map<Long, BigDecimal> retirosPrevistoPorAtm = prediccionDeRetiros.stream()
                .collect(Collectors.toMap(RetiroEfectivoAtmPrediccion::idAtm, RetiroEfectivoAtmPrediccion::retiroPrevisto));
    
        return datosHistoricos.stream()
                .filter(dh -> retirosPrevistoPorAtm.containsKey(dh.idAtm()))
                .map(dh -> new RetiroHistoricoDTO(
                        dh.idAtm(),
                        dh.retiroPromedio(), 
                        retirosPrevistoPorAtm.get(dh.idAtm())
                    )
                )
                .collect(Collectors.toList());
    }

}