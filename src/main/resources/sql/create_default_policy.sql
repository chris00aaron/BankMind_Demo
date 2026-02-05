-- ============================================================
-- Script para crear la política de default inicial
-- BankMind - Módulo de Morosidad
-- Fecha: 2026-02-02
-- ============================================================
-- 
-- Este script crea una política inicial con:
-- - Umbral de default: 50% (threshold_approval = 0.50)
-- - Factor LGD: 45% (estándar para créditos no garantizados)
-- - Matriz SBS basada en PROBABILIDAD DE DEFAULT (valores decimales 0.0-1.0)
--
-- Categorías SBS según Resolución SBS N° 11356-2008:
-- - Normal: Cliente con bajo riesgo de incumplimiento
-- - CPP (Con Problemas Potenciales): Señales de riesgo
-- - Deficiente: Alto riesgo de incumplimiento
-- - Dudoso: Muy alto riesgo
-- - Pérdida: Prácticamente incobrable
-- ============================================================

-- Desactivar cualquier política existente (si hubiera)
UPDATE default_policies SET is_active = false WHERE is_active = true;

-- Insertar política inicial con clasificación SBS basada en probabilidad de default
INSERT INTO default_policies (
    policy_name,
    threshold_approval,
    factor_lgd,
    days_grace_default,
    activation_date,
    cancellation_date,
    is_active,
    approved_by,
    sbs_classification_matrix
) VALUES (
    'Política Estándar SBS v1.0',
    0.5000,                  -- 50% umbral de default (probabilidad)
    0.4500,                  -- 45% LGD (estándar para créditos de consumo no garantizados)
    30,                      -- 30 días de gracia
    CURRENT_DATE,            -- Fecha de activación: hoy
    NULL,                    -- Sin fecha de cancelación (activa)
    true,                    -- Política activa
    'Administrador Sistema', -- Aprobado por
    '[
        {"categoria": "Normal", "min": 0.0, "max": 0.20, "provision": 0.01},
        {"categoria": "CPP", "min": 0.20, "max": 0.40, "provision": 0.05},
        {"categoria": "Deficiente", "min": 0.40, "max": 0.60, "provision": 0.25},
        {"categoria": "Dudoso", "min": 0.60, "max": 0.80, "provision": 0.60},
        {"categoria": "Pérdida", "min": 0.80, "max": 1.0, "provision": 1.0}
    ]'::jsonb
);

-- Verificar inserción
SELECT 
    id_policy AS "ID",
    policy_name AS "Nombre Política",
    threshold_approval * 100 AS "Umbral Default (%)",
    factor_lgd * 100 AS "Factor LGD (%)",
    days_grace_default AS "Días Gracia",
    is_active AS "Activa",
    approved_by AS "Aprobado Por",
    activation_date AS "Fecha Activación"
FROM default_policies
WHERE is_active = true;

-- Mostrar matriz de clasificación SBS
SELECT 
    elem->>'categoria' AS "Categoría SBS",
    (elem->>'min')::numeric * 100 || '% - ' || (elem->>'max')::numeric * 100 || '%' AS "Rango Prob. Default",
    (elem->>'provision')::numeric * 100 || '%' AS "Provisión"
FROM default_policies, jsonb_array_elements(sbs_classification_matrix) AS elem
WHERE is_active = true;
