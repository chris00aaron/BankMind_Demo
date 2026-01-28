package com.naal.bankmind.entity.Default.POJO;

import lombok.Data;
import java.io.Serializable;

@Data
public class ClassficationRuleSBS implements Serializable {
    private String categoria;   // Ej: "CPP"
    private Double min;         // Ej: 0.05
    private Double max;         // Ej: 0.20
    private Double provision;   // Ej: 0.05

    /*
    '[
        {"categoria": "Normal", "min": 0.0, "max": 0.08, "provision": 0.01},
        {"categoria": "CPP", "min": 0.0800001, "max": 0.15, "provision": 0.05},
        {"categoria": "Deficiente", "min": 0.1500001, "max": 0.30, "provision": 0.25},
        {"categoria": "Dudoso", "min": 0.3000001, "max": 0.60, "provision": 0.60},
        {"categoria": "Pérdida", "min": 0.6000001, "max": 1.0, "provision": 1.0}
    ]'::jsonb
    */
}
