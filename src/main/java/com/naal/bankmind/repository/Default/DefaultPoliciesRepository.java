package com.naal.bankmind.repository.Default;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Default.DefaultPolicies;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefaultPoliciesRepository extends JpaRepository<DefaultPolicies, Long> {

    /**
     * Obtiene la política actualmente activa.
     */
    Optional<DefaultPolicies> findByIsActiveTrue();

    /**
     * Lista todas las políticas ordenadas por fecha de activación.
     */
    List<DefaultPolicies> findAllByOrderByActivationDateDesc();
}
