package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.infrastructure.bd.projections.AtmAvgProjection;
import com.naal.bankmind.atm.infrastructure.bd.projections.AtmStatusBalanceProjection;
import com.naal.bankmind.atm.infrastructure.bd.projections.WithdrawalAvgProjection;
import com.naal.bankmind.entity.atm.Atm;

@Repository
public interface JpaAtmRepository extends JpaRepository<Atm, Long> {

    /**
     * Obtiene todos los cajeros activos
     * 
     * @return Lista de cajeros activos
     */
    @Query("""
            SELECT a
            FROM Atm a
            JOIN FETCH a.currentStatus acs
            JOIN FETCH a.locationType lt
            WHERE a.active = true
            """)
    List<Atm> findActiveAtmsWithStatus();


    /**
     * Busca todas las features de un ATM para un día y mes específico
     * 
     * @param day Día del mes que se quiere consultar
     * @param month Mes que se quiere consultar
     * @return Lista de Retiros historicos del ATM para el día y mes especificado
     */
    @Query(value = "SELECT * FROM fn_withdrawal_avg(:day, :month)", nativeQuery = true)
    List<WithdrawalAvgProjection> obtenerRetiroDePromedioHistorico(
        @Param("day") short day,
        @Param("month") short month);

     /**
     * Busca todas las features de un ATM para un día y mes específico
     * 
     * @param day Día del mes que se quiere consultar
     * @param month Mes que se quiere consultar
     * @return Lista de Retiros historicos del ATM para el día y mes especificado
     */
    @Query(value = "SELECT * FROM fn_atm_status_balance(:requested_prediction_date)", nativeQuery = true)
    List<AtmStatusBalanceProjection> obtenerEstadoYSaldoDeCajero(
        @Param("requested_prediction_date") LocalDate predictionDate);

    @Query(value = "SELECT * FROM fn_atm_features_avg(:day, :month)", nativeQuery = true)
    List<AtmAvgProjection> obtenerPromediosAtmFeatures(
        @Param("day") short day,
        @Param("month") short month);
}
