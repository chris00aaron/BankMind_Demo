package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad para la tabla categories
 * Almacena el catálogo de categorías de comercios
 */
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_category")
    private Integer idCategory;

    @Column(name = "category_name", length = 100, unique = true, nullable = false)
    private String categoryName;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
