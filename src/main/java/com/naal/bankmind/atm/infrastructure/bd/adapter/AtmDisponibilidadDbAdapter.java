package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.domain.model.AtmDisponibilidad;
import com.naal.bankmind.atm.domain.ports.out.repository.AtmDisponibilidadRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaAtmRepository;
import com.naal.bankmind.entity.atm.Atm;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@Component
public class AtmDisponibilidadDbAdapter implements AtmDisponibilidadRepository {

    private final JpaAtmRepository atmDisponibilidadRepository;
    
    @Override
    public List<AtmDisponibilidad> obtenerDisponibilidadActual() {

        List<Atm> atms = atmDisponibilidadRepository.findAll();

        return atms.stream()
            .map(atm -> new AtmDisponibilidad(atm.getIdAtm(), atm.isActive()))
            .toList();
    } 
}
