package com.naal.bankmind.atm.infrastructure.bd.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.atm.AtmFeatures;

@Repository
public interface JpaAtmFeaturesRepository extends JpaRepository<AtmFeatures, Long> {

    /**
     * Obtiene todas las características de los cajeros ordenadas por fecha de referencia descendente.
     * @param pageable Información de paginación.
     * @return Página de características de cajeros.
     */
    Page<AtmFeatures> findAllByOrderByReferenceDateDesc(Pageable pageable);
}
