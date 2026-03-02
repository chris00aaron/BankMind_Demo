package com.naal.bankmind.repository.Fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Fraud.Category;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Buscar categoría por nombre
     */
    Optional<Category> findByCategoryName(String categoryName);

    /**
     * Verificar si existe una categoría por nombre
     */
    boolean existsByCategoryName(String categoryName);

    /**
     * Obtener todas las categorías activas ordenadas por nombre
     */
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.categoryName")
    List<Category> findAllActive();

    /**
     * Obtener solo los nombres de categorías activas (para dropdowns)
     */
    @Query("SELECT c.categoryName FROM Category c WHERE c.active = true ORDER BY c.categoryName")
    List<String> findAllCategoryNames();
}
