-- Seed: Política de monitoreo por defecto (valores que antes estaban hardcodeados)
-- Ejecutar manualmente si se desea una política preconfigurada al inicio.

INSERT INTO monitoring_policy (
    policy_name,
    psi_threshold,
    consecutive_days_trigger,
    auc_drop_threshold,
    ks_drop_threshold,
    optuna_trials_drift,
    optuna_trials_validation,
    activation_date,
    cancellation_date,
    is_active,
    created_by
) VALUES (
    'Política Estándar SBS',
    0.2500,
    3,
    0.0500,
    0.1000,
    30,
    50,
    CURRENT_DATE,
    NULL,
    true,
    'Sistema'
);
