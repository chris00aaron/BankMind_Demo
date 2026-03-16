package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.Fraud.ModelFeatureDrift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para model_feature_drift.
 * Provee queries optimizadas para la gráfica de evolución del PSI en el
 * Frontend.
 */
@Repository
public interface ModelFeatureDriftRepository extends JpaRepository<ModelFeatureDrift, Long> {

        /**
         * Obtiene el historial de PSI de un modelo, ordenado por fecha ascendente.
         * Query principal para la gráfica de PSI Evolution.
         */
        List<ModelFeatureDrift> findByModelIdModelOrderByMeasuredAtAsc(Long idModel);

        /**
         * Obtiene las mediciones de drift de los últimos N días para un modelo.
         * Útil para mostrar sólo el período reciente en la gráfica.
         */
        @Query("""
                            SELECT d FROM ModelFeatureDrift d
                            WHERE d.model.idModel = :idModel
                              AND d.measuredAt >= :since
                            ORDER BY d.measuredAt ASC
                        """)
        List<ModelFeatureDrift> findRecentByModel(
                        @Param("idModel") Long idModel,
                        @Param("since") LocalDateTime since);

        /**
         * Obtiene la última medición de PSI por feature para el modelo activo.
         * Usado por el Scheduler para decidir si disparar entrenamiento.
         */
        @Query("""
                            SELECT d FROM ModelFeatureDrift d
                            WHERE d.model.idModel = :idModel
                              AND d.measuredAt = (
                                  SELECT MAX(d2.measuredAt)
                                  FROM ModelFeatureDrift d2
                                  WHERE d2.model.idModel = :idModel AND d2.featureName = d.featureName
                              )
                        """)
        List<ModelFeatureDrift> findLatestPerFeatureByModel(@Param("idModel") Long idModel);

        /**
         * Verifica si algún feature crítico supera el umbral de PSI en la última
         * medición.
         * Retorna true si hay drift severo (PSI > threshold).
         */
        @Query("""
                            SELECT COUNT(d) > 0 FROM ModelFeatureDrift d
                            WHERE d.model.idModel = :idModel
                              AND d.featureName IN :criticalFeatures
                              AND d.driftCategory = 'HIGH'
                              AND d.measuredAt >= :since
                        """)
        boolean existsSevereDriftSince(
                        @Param("idModel") Long idModel,
                        @Param("criticalFeatures") List<String> criticalFeatures,
                        @Param("since") LocalDateTime since);

        // -------------------------------------------------------------------------
        // Cross-model queries (dashboard MLOps — línea de tiempo continua)
        // -------------------------------------------------------------------------

        /**
         * Historial de drift de TODOS los modelos en un rango de fechas.
         * Permite mostrar una línea continua aunque haya cambios de champion.
         */
        @Query("""
                            SELECT d FROM ModelFeatureDrift d
                            WHERE d.measuredAt >= :since
                            ORDER BY d.measuredAt ASC
                        """)
        List<ModelFeatureDrift> findAllSince(@Param("since") LocalDateTime since);

        /**
         * Último PSI medido por feature a nivel global (cross-model).
         * Usa el registro más reciente sin importar a qué modelo pertenece.
         */
        @Query("""
                            SELECT d FROM ModelFeatureDrift d
                            WHERE d.measuredAt = (
                                SELECT MAX(d2.measuredAt)
                                FROM ModelFeatureDrift d2
                                WHERE d2.featureName = d.featureName
                            )
                        """)
        List<ModelFeatureDrift> findLatestPerFeatureGlobal();
}
