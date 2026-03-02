package com.naal.bankmind.atm.application.mapper;

import java.util.List;

import com.naal.bankmind.atm.application.dto.response.ResumenOperativoAtmDTO;
import com.naal.bankmind.atm.domain.model.AtmDisponibilidad;

public class AtmDisponibilidadMapper {

    public static ResumenOperativoAtmDTO toResumenOperativoAtmDTO(List<AtmDisponibilidad> disponibilidades) {
        long activos = 0;
        long inactivos = 0;
        for (AtmDisponibilidad disponibilidad : disponibilidades) {
            if (disponibilidad.activo()) {  activos++;} 
            else {inactivos++;}
        }
        return new ResumenOperativoAtmDTO(activos, inactivos);
    }
}
