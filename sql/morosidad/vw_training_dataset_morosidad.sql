-- =============================================================================
-- Vista Materializada: vw_training_dataset_morosidad
-- Genera el dataset de entrenamiento para el modelo de morosidad
-- usando sliding window de 6 meses (features) + 1 mes (label)
--
-- Ejecutar este script en PgAdmin o psql contra BankMindBetta_v2
-- =============================================================================

-- Eliminar si ya existe
DROP MATERIALIZED VIEW IF EXISTS vw_training_dataset_morosidad;

CREATE MATERIALIZED VIEW vw_training_dataset_morosidad AS
WITH

-- ============================================
-- PASO 1: Filtrar data reciente y numerar meses por cuenta
-- ============================================
numbered AS (
    SELECT
        mh.id_historial,
        mh.record_id,
        mh.monthly_period,
        mh.pay_x,
        mh.bill_amt_x,
        mh.pay_amt_x,
        mh.did_customer_pay,
        mh.expiration_date,
        mh.actual_payment_date,
        ROW_NUMBER() OVER (
            PARTITION BY mh.record_id
            ORDER BY mh.monthly_period ASC
        ) AS rn,
        COUNT(*) OVER (PARTITION BY mh.record_id) AS total_months
    FROM monthly_history mh
    -- Solo data de los últimos 3 años
    WHERE mh.monthly_period >= CURRENT_DATE - INTERVAL '3 years'
),

-- ============================================
-- PASO 2: Generar ventanas deslizantes de 7 meses
-- Para cada cuenta con >= 7 meses, generar ventanas
-- donde rn = posición inicial de la ventana
-- ============================================
windows AS (
    SELECT
        n.record_id,
        n.rn AS window_start,
        -- El mes 7 de la ventana es el mes label
        n.rn + 6 AS window_label_pos
    FROM numbered n
    WHERE n.total_months >= 7
      -- La ventana empieza desde rn=1 hasta rn=(total_months - 6)
      AND n.rn <= n.total_months - 6
    GROUP BY n.record_id, n.rn
),

-- ============================================
-- PASO 3: Pivotar los 6 meses de features + extraer el mes label
-- ============================================
pivoted AS (
    SELECT
        w.record_id,
        w.window_start,

        -- === PAY_0 a PAY_6 (más reciente primero: mes 6 = PAY_0) ===
        MAX(CASE WHEN n.rn = w.window_start + 5 THEN n.pay_x END) AS pay_0,
        MAX(CASE WHEN n.rn = w.window_start + 4 THEN n.pay_x END) AS pay_2,
        MAX(CASE WHEN n.rn = w.window_start + 3 THEN n.pay_x END) AS pay_3,
        MAX(CASE WHEN n.rn = w.window_start + 2 THEN n.pay_x END) AS pay_4,
        MAX(CASE WHEN n.rn = w.window_start + 1 THEN n.pay_x END) AS pay_5,
        MAX(CASE WHEN n.rn = w.window_start     THEN n.pay_x END) AS pay_6,

        -- === BILL_AMT1 a BILL_AMT6 (más reciente primero) ===
        MAX(CASE WHEN n.rn = w.window_start + 5 THEN n.bill_amt_x END) AS bill_amt1,
        MAX(CASE WHEN n.rn = w.window_start + 4 THEN n.bill_amt_x END) AS bill_amt2,
        MAX(CASE WHEN n.rn = w.window_start + 3 THEN n.bill_amt_x END) AS bill_amt3,
        MAX(CASE WHEN n.rn = w.window_start + 2 THEN n.bill_amt_x END) AS bill_amt4,
        MAX(CASE WHEN n.rn = w.window_start + 1 THEN n.bill_amt_x END) AS bill_amt5,
        MAX(CASE WHEN n.rn = w.window_start     THEN n.bill_amt_x END) AS bill_amt6,

        -- === PAY_AMT1 a PAY_AMT6 (más reciente primero) ===
        MAX(CASE WHEN n.rn = w.window_start + 5 THEN n.pay_amt_x END) AS pay_amt1,
        MAX(CASE WHEN n.rn = w.window_start + 4 THEN n.pay_amt_x END) AS pay_amt2,
        MAX(CASE WHEN n.rn = w.window_start + 3 THEN n.pay_amt_x END) AS pay_amt3,
        MAX(CASE WHEN n.rn = w.window_start + 2 THEN n.pay_amt_x END) AS pay_amt4,
        MAX(CASE WHEN n.rn = w.window_start + 1 THEN n.pay_amt_x END) AS pay_amt5,
        MAX(CASE WHEN n.rn = w.window_start     THEN n.pay_amt_x END) AS pay_amt6,

        -- === Datos del mes label (mes 7 de la ventana) ===
        MAX(CASE WHEN n.rn = w.window_label_pos THEN n.expiration_date END) AS label_expiration,
        MAX(CASE WHEN n.rn = w.window_label_pos THEN n.actual_payment_date END) AS label_payment,
        MAX(CASE WHEN n.rn = w.window_label_pos THEN n.monthly_period END) AS label_period,

        -- Período del mes más reciente (para decay)
        MAX(CASE WHEN n.rn = w.window_start + 5 THEN n.monthly_period END) AS feature_period

    FROM windows w
    JOIN numbered n ON n.record_id = w.record_id
                   AND n.rn BETWEEN w.window_start AND w.window_label_pos
    GROUP BY w.record_id, w.window_start
),

-- ============================================
-- PASO 4: Generar label con política activa y calcular decay
-- ============================================
labeled AS (
    SELECT
        p.record_id,
        p.window_start,
        p.pay_0, p.pay_2, p.pay_3, p.pay_4, p.pay_5, p.pay_6,
        p.bill_amt1, p.bill_amt2, p.bill_amt3, p.bill_amt4, p.bill_amt5, p.bill_amt6,
        p.pay_amt1, p.pay_amt2, p.pay_amt3, p.pay_amt4, p.pay_amt5, p.pay_amt6,
        p.feature_period,

        -- Label: ¿fue moroso en el mes siguiente?
        CASE
            -- Si nunca pagó → moroso
            WHEN p.label_payment IS NULL THEN 1
            -- Si el atraso supera los días de gracia → moroso
            WHEN (p.label_payment - p.label_expiration) > COALESCE(dp.days_grace_default, 30) THEN 1
            -- Si no → no moroso
            ELSE 0
        END AS default_payment_next_month,

        -- Sample weight (decay temporal)
        CASE
            WHEN p.feature_period >= CURRENT_DATE - INTERVAL '24 months' THEN 1.0
            ELSE 0.7  -- 24-36 meses (no hay data > 3 años por el filtro)
        END AS sample_weight

    FROM pivoted p
    -- Unir con la política activa en la fecha del label
    LEFT JOIN default_policies dp
        ON dp.is_active = true
        AND p.label_period >= dp.activation_date
        AND (dp.cancellation_date IS NULL OR p.label_period < dp.cancellation_date)

    -- Excluir registros donde el período de gracia aún no venció (no etiquetables)
    WHERE (
        p.label_payment IS NOT NULL
        OR (p.label_expiration + COALESCE(dp.days_grace_default, 30) * INTERVAL '1 day') < CURRENT_DATE
    )
)

-- ============================================
-- PASO 5: Query final con joins demográficos
-- ============================================
SELECT
    l.record_id,
    l.window_start,

    -- Datos de cuenta
    COALESCE(ad.limit_bal, 0) AS limit_bal,

    -- Datos demográficos
    COALESCE(c.id_gender, 1) AS sex,
    COALESCE(c.id_education, 4) AS education,
    COALESCE(c.id_marriage, 3) AS marriage,
    COALESCE(c.age, 30) AS age,

    -- Features de pago (6 meses)
    COALESCE(l.pay_0, 0) AS pay_0,
    COALESCE(l.pay_2, 0) AS pay_2,
    COALESCE(l.pay_3, 0) AS pay_3,
    COALESCE(l.pay_4, 0) AS pay_4,
    COALESCE(l.pay_5, 0) AS pay_5,
    COALESCE(l.pay_6, 0) AS pay_6,

    -- Montos facturados (6 meses)
    COALESCE(l.bill_amt1, 0) AS bill_amt1,
    COALESCE(l.bill_amt2, 0) AS bill_amt2,
    COALESCE(l.bill_amt3, 0) AS bill_amt3,
    COALESCE(l.bill_amt4, 0) AS bill_amt4,
    COALESCE(l.bill_amt5, 0) AS bill_amt5,
    COALESCE(l.bill_amt6, 0) AS bill_amt6,

    -- Montos pagados (6 meses)
    COALESCE(l.pay_amt1, 0) AS pay_amt1,
    COALESCE(l.pay_amt2, 0) AS pay_amt2,
    COALESCE(l.pay_amt3, 0) AS pay_amt3,
    COALESCE(l.pay_amt4, 0) AS pay_amt4,
    COALESCE(l.pay_amt5, 0) AS pay_amt5,
    COALESCE(l.pay_amt6, 0) AS pay_amt6,

    -- Utilization rate
    CASE
        WHEN COALESCE(ad.limit_bal, 0) > 0
        THEN ROUND(COALESCE(l.bill_amt1, 0) / ad.limit_bal, 4)
        ELSE 0
    END AS utilization_rate,

    -- Label y peso
    l.default_payment_next_month,
    l.sample_weight

FROM labeled l
JOIN account_details ad ON ad.record_id = l.record_id
JOIN customer c ON c.id_customer = ad.id_customer
ORDER BY l.record_id, l.window_start;

-- Crear índice para consultas rápidas
CREATE UNIQUE INDEX idx_vw_training_record_window
ON vw_training_dataset_morosidad (record_id, window_start);

-- =============================================================================
-- Para refrescar la vista antes de cada entrenamiento:
-- REFRESH MATERIALIZED VIEW vw_training_dataset_morosidad;
-- =============================================================================
