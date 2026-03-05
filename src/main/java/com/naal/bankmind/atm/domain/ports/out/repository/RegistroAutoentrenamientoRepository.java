package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.Optional;

import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.RegistroAutoentrenamiento;

public interface RegistroAutoentrenamientoRepository {

    /**
     * Busca un registro de autoentrenamiento por su ID.
     * @param id El ID del registro de autoentrenamiento.
     * @return Un Optional que contiene el registro de autoentrenamiento si se encuentra, o un Optional vacío si no se encuentra.
     */
    Optional<RegistroAutoentrenamiento> findById(Long id);

    /**
     * Obtiene todos los registros de autoentrenamiento de forma paginada.
     * @param page El número de página.
     * @param size El tamaño de la página.
     * @return Una página de registros de autoentrenamiento.
     */
    PageResult<RegistroAutoentrenamiento> findAll(int page, int size);
}
