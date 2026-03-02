package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.ProductionModelDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionModelDefaultRepository extends JpaRepository<ProductionModelDefault, Long> {

    /**
     * Retira todos los modelos activos (set is_active=false y retire_date=now).
     */
    @Modifying
    @Query("UPDATE ProductionModelDefault p SET p.isActive = false, p.retireDate = CURRENT_TIMESTAMP WHERE p.isActive = true")
    void retireAllActiveModels();

    /**
     * Obtiene el modelo activo en producción.
     */
    Optional<ProductionModelDefault> findByIsActiveTrue();

    /**
     * Obtiene un modelo por su versión.
     */
    Optional<ProductionModelDefault> findByVersion(String version);
}
