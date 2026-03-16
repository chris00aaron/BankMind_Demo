-- ============================================
-- ÍNDICES DE RENDIMIENTO - MÓDULO MOROSIDAD
-- BankMind - 2026-02-17
-- ============================================
-- Ejecutar en PostgreSQL con la BD activa.
-- CONCURRENTLY permite crear índices sin bloquear tablas.

-- 1. MONTHLY_HISTORY: record_id + monthly_period DESC
--    Impacto: Dashboard, Batch, Alertas (las 3 vistas)
--    Queries: findAllByRecordIds(), ROW_NUMBER() OVER PARTITION BY record_id,
--             obtenerCuotasAtrasadasBatch()
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mh_record_period
ON monthly_history (record_id, monthly_period DESC);

-- 2. DEFAULT_PREDICTION: id_historial + date_prediction DESC
--    Impacto: Dashboard (getLatestPredictionsPerAccount)
--    Query: ROW_NUMBER() OVER (PARTITION BY record_id ORDER BY date_prediction DESC)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dp_historial_date
ON default_prediction (id_historial, date_prediction DESC);

-- 3. DEFAULT_PREDICTION: default_probability
--    Impacto: Cálculo de percentil de riesgo
--    Query: countByDefaultProbabilityLessThan()
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dp_probability
ON default_prediction (default_probability);

-- 4. MONTHLY_HISTORY: Covering index para tendencia mensual
--    Impacto: Dashboard (calcularTendenciaMensualOptimizado)
--    Query: GROUP BY monthly_period con SUM(pay_x)
--    INCLUDE evita acceso a tabla (index-only scan)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mh_period_covering
ON monthly_history (monthly_period) INCLUDE (record_id, pay_x);

-- ============================================
-- VERIFICACIÓN: Ejecutar después de crear índices
-- ============================================
-- EXPLAIN ANALYZE
-- SELECT * FROM monthly_history
-- WHERE record_id IN (1, 2, 3)
-- ORDER BY monthly_period DESC;
-- -> Debe mostrar "Index Scan" en lugar de "Seq Scan"
