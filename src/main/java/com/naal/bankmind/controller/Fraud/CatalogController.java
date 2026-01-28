package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.entity.Category;
import com.naal.bankmind.repository.Fraud.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para catálogos del sistema
 * Proporciona endpoints para obtener datos de referencia
 */
@RestController
@RequestMapping("/api/catalog")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
public class CatalogController {

    private final CategoryRepository categoryRepository;

    public CatalogController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * GET /api/catalog/categories - Obtener todas las categorías activas
     * Usado para poblar dropdowns en el frontend
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        List<Category> categories = categoryRepository.findAllActive();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/catalog/categories/names - Obtener solo los nombres de categorías
     * Versión ligera para dropdowns simples
     */
    @GetMapping("/categories/names")
    public ResponseEntity<List<String>> getCategoryNames() {
        List<String> names = categoryRepository.findAllCategoryNames();
        return ResponseEntity.ok(names);
    }
}
