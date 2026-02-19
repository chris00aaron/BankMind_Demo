package com.naal.bankmind.config.Default;

import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.MonthlyHistory;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.MonthlyHistoryRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Servicio que genera datos sintéticos en monthly_history para
 * probar el auto-retraining con sliding window.
 *
 * Divide las cuentas en dos grupos:
 * - 50% → 12 meses de historial (Feb 2025 – Ene 2026)
 * - 50% → 7 meses de historial (Jul 2025 – Ene 2026)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyHistoryGenerator {

    private final AccountDetailsRepository accountDetailsRepository;
    private final MonthlyHistoryRepository monthlyHistoryRepository;
    private final EntityManager entityManager;

    private static final LocalDate START_12_MONTHS = LocalDate.of(2025, 2, 1);
    private static final LocalDate START_7_MONTHS = LocalDate.of(2025, 7, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 1, 1);

    // Distribución de perfiles de pago
    private static final double PROFILE_GOOD_PAYER = 0.60; // 60% buenos pagadores
    private static final double PROFILE_IRREGULAR = 0.25; // 25% pagadores irregulares
    // El 15% restante serán morosos

    /**
     * Genera datos sintéticos para todas las cuentas.
     * Retorna un mapa con estadísticas del proceso.
     */
    @Transactional
    public Map<String, Object> generarDatosSinteticos() {
        log.info("🚀 Iniciando generación de datos sintéticos...");

        // 0. Resetear la secuencia de PKs para evitar conflictos
        resetearSecuencia();

        // 1. Cargar todas las cuentas (solo IDs y limitBal, sin lazy loading)
        log.info("📊 Cargando cuentas...");
        List<AccountDetails> todasLasCuentas = accountDetailsRepository.findAll();
        log.info("📊 Total de cuentas encontradas: {}", todasLasCuentas.size());

        if (todasLasCuentas.isEmpty()) {
            log.warn("⚠️ No se encontraron cuentas en la base de datos");
            return Map.of("error", "No se encontraron cuentas", "totalAccounts", 0);
        }

        // 2. Pre-cargar TODOS los períodos existentes en memoria (una sola query)
        log.info("🔍 Pre-cargando períodos existentes para evitar duplicados...");
        Set<String> existentes = precargarPeriodosExistentes();
        log.info("🔍 Períodos existentes cargados: {}", existentes.size());

        // 3. Mezclar y dividir en dos grupos
        List<AccountDetails> cuentasMezcladas = new ArrayList<>(todasLasCuentas);
        Collections.shuffle(cuentasMezcladas, new Random(42)); // Seed fijo para reproducibilidad

        int mitad = cuentasMezcladas.size() / 2;
        List<AccountDetails> grupo12Meses = cuentasMezcladas.subList(0, mitad);
        List<AccountDetails> grupo7Meses = cuentasMezcladas.subList(mitad, cuentasMezcladas.size());

        log.info("📂 Grupo 12 meses: {} cuentas | Grupo 7 meses: {} cuentas",
                grupo12Meses.size(), grupo7Meses.size());

        // 4. Generar data para cada grupo
        Random random = new Random(123);
        int totalGenerados = 0;
        int totalDuplicadosOmitidos = 0;

        log.info("📝 Generando historial de 12 meses (Feb 2025 – Ene 2026)...");
        int[] result12 = generarParaGrupo(grupo12Meses, START_12_MONTHS, END_DATE, random, existentes);
        totalGenerados += result12[0];
        totalDuplicadosOmitidos += result12[1];

        log.info("📝 Generando historial de 7 meses (Jul 2025 – Ene 2026)...");
        int[] result7 = generarParaGrupo(grupo7Meses, START_7_MONTHS, END_DATE, random, existentes);
        totalGenerados += result7[0];
        totalDuplicadosOmitidos += result7[1];

        log.info("✅ Generación completada. Total registros: {} | Duplicados omitidos: {}",
                totalGenerados, totalDuplicadosOmitidos);

        // 5. Retornar estadísticas
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAccounts", todasLasCuentas.size());
        stats.put("group12Months", grupo12Meses.size());
        stats.put("group7Months", grupo7Meses.size());
        stats.put("totalRecordsGenerated", totalGenerados);
        stats.put("duplicatesSkipped", totalDuplicadosOmitidos);
        stats.put("dateRange", START_12_MONTHS + " to " + END_DATE);
        return stats;
    }

    /**
     * Resetea la secuencia de monthly_history para que no colisione
     * con registros existentes.
     */
    private void resetearSecuencia() {
        try {
            entityManager.createNativeQuery(
                    "SELECT setval('monthly_history_id_historial_seq', " +
                            "(SELECT COALESCE(MAX(id_historial), 0) FROM monthly_history))")
                    .getSingleResult();
            log.info("🔧 Secuencia de monthly_history reseteada correctamente");
        } catch (Exception e) {
            log.warn("⚠️ No se pudo resetear la secuencia: {}. Intentando con nombre alternativo...",
                    e.getMessage());
            try {
                // Intentar con nombre de secuencia por defecto de Hibernate
                entityManager.createNativeQuery(
                        "SELECT setval(pg_get_serial_sequence('monthly_history', 'id_historial'), " +
                                "(SELECT COALESCE(MAX(id_historial), 0) FROM monthly_history))")
                        .getSingleResult();
                log.info("🔧 Secuencia reseteada con nombre alternativo");
            } catch (Exception e2) {
                log.error("❌ No se pudo resetear la secuencia: {}", e2.getMessage());
            }
        }
    }

    /**
     * Pre-carga todos los pares (record_id, monthly_period) existentes
     * en un Set para verificación O(1) en memoria.
     * Una sola query en vez de N×M queries individuales.
     */
    @SuppressWarnings("unchecked")
    private Set<String> precargarPeriodosExistentes() {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT record_id, monthly_period FROM monthly_history")
                .getResultList();

        Set<String> existentes = new HashSet<>(rows.size());
        for (Object[] row : rows) {
            Long recordId = ((Number) row[0]).longValue();
            LocalDate period = ((java.sql.Date) row[1]).toLocalDate();
            existentes.add(recordId + "_" + period);
        }
        return existentes;
    }

    /**
     * Genera registros de monthly_history para un grupo de cuentas en un rango de
     * fechas.
     * Retorna [registrosGenerados, duplicadosOmitidos].
     */
    private int[] generarParaGrupo(List<AccountDetails> cuentas, LocalDate startDate,
            LocalDate endDate, Random random, Set<String> existentes) {
        int generados = 0;
        int duplicados = 0;
        List<MonthlyHistory> batch = new ArrayList<>();
        int cuentasProcesadas = 0;

        for (AccountDetails cuenta : cuentas) {
            // Asignar un perfil de pago consistente a esta cuenta
            PaymentProfile profile = asignarPerfil(random);

            LocalDate currentMonth = startDate;
            while (!currentMonth.isAfter(endDate)) {
                // Verificar duplicado en memoria (O(1)) en vez de query a BD
                String key = cuenta.getRecordId() + "_" + currentMonth;
                if (existentes.contains(key)) {
                    duplicados++;
                    currentMonth = currentMonth.plusMonths(1);
                    continue;
                }

                MonthlyHistory record = generarRegistro(cuenta, currentMonth, profile, random);
                batch.add(record);
                existentes.add(key); // Marcar como existente para futuras verificaciones
                generados++;

                // Guardar en lotes de 500 y limpiar contexto de persistencia
                if (batch.size() >= 500) {
                    monthlyHistoryRepository.saveAll(batch);
                    entityManager.flush();
                    entityManager.clear();
                    batch.clear();
                }

                currentMonth = currentMonth.plusMonths(1);
            }

            cuentasProcesadas++;
            if (cuentasProcesadas % 5000 == 0) {
                log.info("📊 Progreso: {}/{} cuentas procesadas | {} registros generados",
                        cuentasProcesadas, cuentas.size(), generados);
            }
        }

        // Guardar registros restantes
        if (!batch.isEmpty()) {
            monthlyHistoryRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        }

        return new int[] { generados, duplicados };
    }

    /**
     * Genera un registro individual de MonthlyHistory con datos realistas.
     */
    private MonthlyHistory generarRegistro(AccountDetails cuenta, LocalDate month,
            PaymentProfile profile, Random random) {
        MonthlyHistory record = new MonthlyHistory();
        record.setAccountDetails(cuenta);
        record.setMonthlyPeriod(month);

        // Generar estado de pago según el perfil
        int payX = generarPayX(profile, random);
        record.setPayX(payX);

        // Generar monto facturado (entre 5% y 80% del límite de crédito)
        BigDecimal limitBal = cuenta.getLimitBal();
        if (limitBal == null || limitBal.compareTo(BigDecimal.ZERO) <= 0) {
            limitBal = new BigDecimal("100000"); // Fallback
        }
        double factorFactura = 0.05 + (random.nextDouble() * 0.75); // 5% a 80%
        BigDecimal billAmt = limitBal.multiply(BigDecimal.valueOf(factorFactura))
                .setScale(2, RoundingMode.HALF_UP);
        record.setBillAmtX(billAmt);

        // Generar monto pagado según estado de pago
        BigDecimal payAmt = generarPayAmt(payX, billAmt, random);
        record.setPayAmtX(payAmt);

        // ¿Pagó algo?
        boolean didPay = payAmt.compareTo(BigDecimal.ZERO) > 0;
        record.setDidCustomerPay(didPay);

        // Fecha de vencimiento: día 15 del mes
        LocalDate expirationDate = month.withDayOfMonth(15);
        record.setExpirationDate(expirationDate);

        // Fecha de pago real
        LocalDate actualPaymentDate = generarActualPaymentDate(payX, expirationDate, didPay, random);
        record.setActualPaymentDate(actualPaymentDate);

        return record;
    }

    /**
     * Genera el estado de pago (payX) según el perfil:
     * -2: pagó todo 2 meses antes
     * -1: pagó todo 1 mes antes
     * 0: pago mínimo a tiempo
     * 1: retraso 1 mes
     * 2+: retraso severo
     */
    private int generarPayX(PaymentProfile profile, Random random) {
        double r = random.nextDouble();

        switch (profile) {
            case GOOD:
                // 80% pago anticipado, 15% a tiempo, 5% retraso leve
                if (r < 0.40)
                    return -2;
                if (r < 0.80)
                    return -1;
                if (r < 0.95)
                    return 0;
                return 1;

            case IRREGULAR:
                // 30% anticipado, 25% a tiempo, 25% retraso 1 mes, 15% retraso 2, 5% peor
                if (r < 0.15)
                    return -2;
                if (r < 0.30)
                    return -1;
                if (r < 0.55)
                    return 0;
                if (r < 0.80)
                    return 1;
                if (r < 0.95)
                    return 2;
                return 3;

            case DELINQUENT:
                // 10% a tiempo, 20% retraso 1, 30% retraso 2, 25% retraso 3, 15% peor
                if (r < 0.05)
                    return -1;
                if (r < 0.10)
                    return 0;
                if (r < 0.30)
                    return 1;
                if (r < 0.60)
                    return 2;
                if (r < 0.85)
                    return 3;
                return 4;

            default:
                return 0;
        }
    }

    /**
     * Genera el monto pagado según el estado de pago.
     */
    private BigDecimal generarPayAmt(int payX, BigDecimal billAmt, Random random) {
        if (payX <= -1) {
            // Pagó a tiempo o anticipado: paga entre 80% y 110% del facturado
            double factor = 0.80 + (random.nextDouble() * 0.30);
            return billAmt.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        } else if (payX == 0) {
            // Pago mínimo: entre 20% y 50%
            double factor = 0.20 + (random.nextDouble() * 0.30);
            return billAmt.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        } else if (payX <= 2) {
            // Retraso leve: pago parcial entre 5% y 30%
            double factor = 0.05 + (random.nextDouble() * 0.25);
            return billAmt.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Retraso severo: ~80% no paga nada, ~20% paga algo mínimo
            if (random.nextDouble() < 0.80) {
                return BigDecimal.ZERO;
            }
            double factor = 0.01 + (random.nextDouble() * 0.10);
            return billAmt.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Genera la fecha de pago real según el estado de pago.
     * - Pagó a tiempo (payX <= 0): entre el 1 y 15 del mes
     * - Se retrasó (payX >= 1): expirationDate + (payX × 30) días
     * - Nunca pagó (didPay = false): null
     */
    private LocalDate generarActualPaymentDate(int payX, LocalDate expirationDate,
            boolean didPay, Random random) {
        if (!didPay) {
            return null; // Nunca pagó
        }

        if (payX <= 0) {
            // Pagó entre el día 1 y el día 15 del mes
            int dayOfPayment = 1 + random.nextInt(15);
            return expirationDate.withDayOfMonth(dayOfPayment);
        } else {
            // Se retrasó: expirationDate + (payX × 30) días con algo de variación
            int daysLate = (payX * 30) + random.nextInt(15); // Variación de hasta 15 días
            return expirationDate.plusDays(daysLate);
        }
    }

    /**
     * Asigna un perfil de pago a una cuenta.
     */
    private PaymentProfile asignarPerfil(Random random) {
        double r = random.nextDouble();
        if (r < PROFILE_GOOD_PAYER)
            return PaymentProfile.GOOD;
        if (r < PROFILE_GOOD_PAYER + PROFILE_IRREGULAR)
            return PaymentProfile.IRREGULAR;
        return PaymentProfile.DELINQUENT;
    }

    /**
     * Perfiles de comportamiento de pago.
     */
    private enum PaymentProfile {
        GOOD, // Buen pagador: casi siempre paga a tiempo
        IRREGULAR, // Pagador irregular: a veces se retrasa
        DELINQUENT // Moroso: frecuentemente se retrasa o no paga
    }
}
