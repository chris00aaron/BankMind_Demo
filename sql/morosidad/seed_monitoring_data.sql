-- =============================================================================
-- DATOS SINTÉTICOS DE MONITOREO
-- Para poblar la tabla model_monitoring_log con datos realistas
-- que permitan ver las gráficas de Drift (PSI) y Validación (Pred vs Real)
-- 
-- NARRATIVA:
--   - Los primeros ~20 días el modelo está estable (PSI bajo)
--   - Del día 21 al 25, PAY_0 y BILL_AMT1 empiezan a crecer (cambio en
--     la distribución — posible cambio de política de cobro)
--   - Del día 26 al 30, se cruza el umbral 0.25 → drift_detected = true
--   - Esto provocaría auto-retraining a los 3 días consecutivos
--
--   - Para validación mensual: 6 meses donde al inicio predicción ≈ realidad,
--     pero en los últimos meses diverge ligeramente (degradación natural)
-- =============================================================================

-- Limpiar datos anteriores de prueba
DELETE FROM model_monitoring_log;

-- ═══════════════════════════════════════════
-- PARTE 1: PSI DIARIO (30 días) — Data Drift
-- ═══════════════════════════════════════════
-- Features monitoreadas: PAY_0, PAY_2, LIMIT_BAL, BILL_AMT1, UTILIZATION_RATE, PAY_AMT1

INSERT INTO model_monitoring_log (monitoring_date, id_production_model, id_monitoring_policy, psi_features, drift_detected, consecutive_days_drift, validation_status, created_at) VALUES

-- Semana 1: Todo estable — PSI muy bajo (< 0.10)
('2026-02-12', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.012, "PAY_2": 0.008, "LIMIT_BAL": 0.015, "BILL_AMT1": 0.022, "UTILIZATION_RATE": 0.018, "PAY_AMT1": 0.009}', false, 0, 'PENDING', '2026-02-12 06:00:00'),
('2026-02-13', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.015, "PAY_2": 0.011, "LIMIT_BAL": 0.013, "BILL_AMT1": 0.019, "UTILIZATION_RATE": 0.016, "PAY_AMT1": 0.012}', false, 0, 'PENDING', '2026-02-13 06:00:00'),
('2026-02-14', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.018, "PAY_2": 0.009, "LIMIT_BAL": 0.011, "BILL_AMT1": 0.025, "UTILIZATION_RATE": 0.014, "PAY_AMT1": 0.008}', false, 0, 'PENDING', '2026-02-14 06:00:00'),
('2026-02-15', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.021, "PAY_2": 0.013, "LIMIT_BAL": 0.017, "BILL_AMT1": 0.020, "UTILIZATION_RATE": 0.019, "PAY_AMT1": 0.011}', false, 0, 'PENDING', '2026-02-15 06:00:00'),
('2026-02-16', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.014, "PAY_2": 0.010, "LIMIT_BAL": 0.014, "BILL_AMT1": 0.023, "UTILIZATION_RATE": 0.017, "PAY_AMT1": 0.010}', false, 0, 'PENDING', '2026-02-16 06:00:00'),
('2026-02-17', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.019, "PAY_2": 0.012, "LIMIT_BAL": 0.016, "BILL_AMT1": 0.021, "UTILIZATION_RATE": 0.015, "PAY_AMT1": 0.013}', false, 0, 'PENDING', '2026-02-17 06:00:00'),
('2026-02-18', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.016, "PAY_2": 0.014, "LIMIT_BAL": 0.012, "BILL_AMT1": 0.024, "UTILIZATION_RATE": 0.020, "PAY_AMT1": 0.011}', false, 0, 'PENDING', '2026-02-18 06:00:00'),

-- Semana 2: Sigue estable, pequeñas fluctuaciones normales
('2026-02-19', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.023, "PAY_2": 0.015, "LIMIT_BAL": 0.018, "BILL_AMT1": 0.028, "UTILIZATION_RATE": 0.021, "PAY_AMT1": 0.014}', false, 0, 'PENDING', '2026-02-19 06:00:00'),
('2026-02-20', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.025, "PAY_2": 0.013, "LIMIT_BAL": 0.015, "BILL_AMT1": 0.031, "UTILIZATION_RATE": 0.019, "PAY_AMT1": 0.012}', false, 0, 'PENDING', '2026-02-20 06:00:00'),
('2026-02-21', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.020, "PAY_2": 0.016, "LIMIT_BAL": 0.019, "BILL_AMT1": 0.027, "UTILIZATION_RATE": 0.022, "PAY_AMT1": 0.015}', false, 0, 'PENDING', '2026-02-21 06:00:00'),
('2026-02-22', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.028, "PAY_2": 0.014, "LIMIT_BAL": 0.014, "BILL_AMT1": 0.033, "UTILIZATION_RATE": 0.024, "PAY_AMT1": 0.013}', false, 0, 'PENDING', '2026-02-22 06:00:00'),
('2026-02-23', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.032, "PAY_2": 0.017, "LIMIT_BAL": 0.016, "BILL_AMT1": 0.029, "UTILIZATION_RATE": 0.020, "PAY_AMT1": 0.016}', false, 0, 'PENDING', '2026-02-23 06:00:00'),
('2026-02-24', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.030, "PAY_2": 0.015, "LIMIT_BAL": 0.017, "BILL_AMT1": 0.035, "UTILIZATION_RATE": 0.023, "PAY_AMT1": 0.014}', false, 0, 'PENDING', '2026-02-24 06:00:00'),
('2026-02-25', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.035, "PAY_2": 0.018, "LIMIT_BAL": 0.015, "BILL_AMT1": 0.032, "UTILIZATION_RATE": 0.025, "PAY_AMT1": 0.017}', false, 0, 'PENDING', '2026-02-25 06:00:00'),

-- Semana 3: Empieza a notarse algo — PAY_0 sube gradualmente
-- (Simulando un cambio de política o comportamiento estacional)
('2026-02-26', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.045, "PAY_2": 0.019, "LIMIT_BAL": 0.018, "BILL_AMT1": 0.038, "UTILIZATION_RATE": 0.026, "PAY_AMT1": 0.015}', false, 0, 'PENDING', '2026-02-26 06:00:00'),
('2026-02-27', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.058, "PAY_2": 0.021, "LIMIT_BAL": 0.016, "BILL_AMT1": 0.042, "UTILIZATION_RATE": 0.028, "PAY_AMT1": 0.018}', false, 0, 'PENDING', '2026-02-27 06:00:00'),
('2026-02-28', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.072, "PAY_2": 0.023, "LIMIT_BAL": 0.019, "BILL_AMT1": 0.055, "UTILIZATION_RATE": 0.031, "PAY_AMT1": 0.016}', false, 0, 'PENDING', '2026-02-28 06:00:00'),
('2026-03-01', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.089, "PAY_2": 0.025, "LIMIT_BAL": 0.017, "BILL_AMT1": 0.068, "UTILIZATION_RATE": 0.034, "PAY_AMT1": 0.019}', false, 0, 'PENDING', '2026-03-01 06:00:00'),
('2026-03-02', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.105, "PAY_2": 0.028, "LIMIT_BAL": 0.020, "BILL_AMT1": 0.078, "UTILIZATION_RATE": 0.037, "PAY_AMT1": 0.017}', false, 0, 'PENDING', '2026-03-02 06:00:00'),
('2026-03-03', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.125, "PAY_2": 0.030, "LIMIT_BAL": 0.018, "BILL_AMT1": 0.092, "UTILIZATION_RATE": 0.040, "PAY_AMT1": 0.021}', false, 0, 'PENDING', '2026-03-03 06:00:00'),
('2026-03-04', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.148, "PAY_2": 0.033, "LIMIT_BAL": 0.021, "BILL_AMT1": 0.108, "UTILIZATION_RATE": 0.043, "PAY_AMT1": 0.020}', false, 0, 'PENDING', '2026-03-04 06:00:00'),

-- Semana 4: ALERTA — PAY_0 cruza umbral 0.25, drift detectado
-- BILL_AMT1 también sube peligrosamente
('2026-03-05', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.175, "PAY_2": 0.036, "LIMIT_BAL": 0.019, "BILL_AMT1": 0.128, "UTILIZATION_RATE": 0.048, "PAY_AMT1": 0.023}', false, 0, 'PENDING', '2026-03-05 06:00:00'),
('2026-03-06', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.198, "PAY_2": 0.039, "LIMIT_BAL": 0.022, "BILL_AMT1": 0.152, "UTILIZATION_RATE": 0.052, "PAY_AMT1": 0.025}', false, 0, 'PENDING', '2026-03-06 06:00:00'),
('2026-03-07', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.228, "PAY_2": 0.042, "LIMIT_BAL": 0.020, "BILL_AMT1": 0.178, "UTILIZATION_RATE": 0.058, "PAY_AMT1": 0.022}', false, 0, 'PENDING', '2026-03-07 06:00:00'),
-- ⚡ PAY_0 cruza 0.25 → drift_detected = true
('2026-03-08', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.262, "PAY_2": 0.045, "LIMIT_BAL": 0.023, "BILL_AMT1": 0.198, "UTILIZATION_RATE": 0.064, "PAY_AMT1": 0.027}', true, 1, 'PENDING', '2026-03-08 06:00:00'),
('2026-03-09', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.298, "PAY_2": 0.048, "LIMIT_BAL": 0.021, "BILL_AMT1": 0.221, "UTILIZATION_RATE": 0.069, "PAY_AMT1": 0.024}', true, 2, 'PENDING', '2026-03-09 06:00:00'),
('2026-03-10', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.335, "PAY_2": 0.052, "LIMIT_BAL": 0.024, "BILL_AMT1": 0.248, "UTILIZATION_RATE": 0.075, "PAY_AMT1": 0.029}', true, 3, 'PENDING', '2026-03-10 06:00:00'),
-- ⚡ 3 días consecutivos → se dispara auto-retraining
('2026-03-11', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.312, "PAY_2": 0.046, "LIMIT_BAL": 0.022, "BILL_AMT1": 0.235, "UTILIZATION_RATE": 0.068, "PAY_AMT1": 0.026}', true, 4, 'PENDING', '2026-03-11 06:00:00'),

-- Post retraining: PSI baja (nuevo modelo absorbe la nueva distribución)
('2026-03-12', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.045, "PAY_2": 0.012, "LIMIT_BAL": 0.014, "BILL_AMT1": 0.038, "UTILIZATION_RATE": 0.019, "PAY_AMT1": 0.011}', false, 0, 'PENDING', '2026-03-12 06:00:00'),
('2026-03-13', (SELECT MAX(id_production_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{"PAY_0": 0.032, "PAY_2": 0.010, "LIMIT_BAL": 0.012, "BILL_AMT1": 0.028, "UTILIZATION_RATE": 0.016, "PAY_AMT1": 0.009}', false, 0, 'PENDING', '2026-03-13 06:00:00');


-- ═══════════════════════════════════════════
-- PARTE 2: VALIDACIÓN MENSUAL — Predicción vs Realidad
-- ═══════════════════════════════════════════

INSERT INTO model_monitoring_log (monitoring_date, id_production_model, id_monitoring_policy, psi_features, drift_detected, consecutive_days_drift, validation_status, auc_roc_real, ks_real, predicted_default_rate, actual_default_rate, created_at) VALUES

-- Hace 6 meses
('2025-10-01', (SELECT MAX(id_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{}', false, 0, 'VALIDATED', 0.8920, 0.6180, 0.0620, 0.0595, '2025-10-01 06:00:00'),

-- Hace 5 meses
('2025-11-01', (SELECT MAX(id_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{}', false, 0, 'VALIDATED', 0.8875, 0.6050, 0.0610, 0.0580, '2025-11-01 06:00:00'),

-- Hace 4 meses
('2025-12-01', (SELECT MAX(id_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{}', false, 0, 'VALIDATED', 0.8830, 0.5920, 0.0640, 0.0685, '2025-12-01 06:00:00'),

-- Hace 3 meses
('2026-01-01', (SELECT MAX(id_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{}', false, 0, 'VALIDATED', 0.8690, 0.5680, 0.0660, 0.0820, '2026-01-01 06:00:00'),

-- Hace 2 meses
('2026-02-01', (SELECT MAX(id_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{}', false, 0, 'VALIDATED', 0.8540, 0.5420, 0.0680, 0.0875, '2026-02-01 06:00:00'),

-- Hace 1 mes
('2026-03-01', (SELECT MAX(id_model) FROM production_model_default), (SELECT MAX(id_monitoring_policy) FROM monitoring_policy), '{}', false, 0, 'VALIDATED', 0.8950, 0.6250, 0.0780, 0.0745, '2026-03-01 06:00:00');


-- =============================================================================
-- GUÍA DE INTERPRETACIÓN PARA EL PROFESOR
-- =============================================================================
--
-- 📊 GRÁFICA PSI (Data Drift):
-- ────────────────────────────
-- • PSI (Population Stability Index) mide cuánto cambió la distribución
--   de cada variable respecto a la distribución de entrenamiento.
-- • PSI < 0.10 → Distribución estable (sin cambio significativo)
-- • PSI 0.10-0.25 → Cambio moderado (monitorear)
-- • PSI > 0.25 → Cambio significativo (DRIFT detectado)
--
-- EN LOS DATOS:
--   Días 1-14: Todas las variables mantienen PSI < 0.04 → modelo estable.
--   Días 15-21: PAY_0 empieza a subir (0.04 → 0.15) indicando que
--               la distribución de estado de pago está cambiando.
--               BILL_AMT1 también sube pero más lento.
--   Días 22-28: PAY_0 cruza el umbral 0.25 → DRIFT CONFIRMADO.
--               Esto podría deberse a:
--               - Cambio en política de cobro del banco
--               - Efecto estacional (fin de año, campaña navideña)
--               - Nuevo segmento de clientes incorporado
--   Días 29-30: Post-retraining → PSI vuelve a niveles normales porque
--               el nuevo modelo usa distribuciones actualizadas como baseline.
--
-- 📈 GRÁFICA PREDICCIÓN VS REALIDAD:
-- ───────────────────────────────────
-- • Compara la tasa de morosidad predicha por el modelo vs la tasa
--   que realmente se observó en ese mes.
-- • Si las líneas están juntas → modelo bien calibrado.
-- • Si divergen → modelo está desactualizado.
--
-- EN LOS DATOS:
--   Sep-Oct 2025: Líneas casi superpuestas (diferencia < 0.5%).
--                 El modelo predice bien la realidad.
--   Nov 2025: Diferencia sube a ~0.5% — aceptable pero creciendo.
--   Dic 2025: La tasa real (8.2%) supera a la predicha (6.6%).
--             Causa: efecto navideño que el modelo no captura.
--   Ene 2026: La brecha persiste → señal de degradación.
--             AUC-ROC real baja de 0.892 a 0.854.
--   Feb 2026: Post-retraining → las líneas se vuelven a juntar.
--             AUC-ROC sube a 0.895 confirmando recuperación.
--
-- 🔄 CICLO COMPLETO DEMOSTRADO:
--   1. Modelo estable con buen rendimiento
--   2. Cambio externo provoca drift en variables clave
--   3. Monitoreo detecta drift (PSI > 0.25)
--   4. Validación mensual confirma degradación (AUC baja, brecha sube)
--   5. Auto-retraining corrige el modelo
--   6. Post-retraining: PSI y brecha vuelven a niveles normales
-- =============================================================================
