package com.naal.bankmind.repository.atm;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.naal.bankmind.dto.atm.projection.AtmAvgProjectionDTO;
import com.naal.bankmind.entity.atm.AtmFeatures;


public interface AtmFeaturesRepository extends JpaRepository<AtmFeatures, Long> {

    /**
     * Busca todas las features de un ATM para un día y mes específico
     * 
     * @param dayOfMonth Día del mes
     * @param month Mes
     * @return Lista de features historicos del ATM para el día y mes especificado
     */
    @Query(value = "SELECT * FROM fn_atm_features_avg(:day, :month)", 
          nativeQuery = true)
    List<AtmAvgProjectionDTO> obtenerPromedios(
            @Param("day") short day,
            @Param("month") short month);
    
}
