package com.naal.bankmind.config.Churn;

import com.naal.bankmind.entity.RetentionSegmentDef;
import com.naal.bankmind.entity.RetentionStrategyDef;
import com.naal.bankmind.repository.Churn.RetentionSegmentDefRepository;
import com.naal.bankmind.repository.Churn.RetentionStrategyDefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeder especializado para el módulo de Churn.
 * Carga estrategias y segmentos de retención iniciales.
 * Incluye validación de existencia de clientes para los segmentos definidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Se ejecuta después del DataSeeder de Seguridad
public class ChurnDataSeeder implements CommandLineRunner {

    private final RetentionStrategyDefRepository strategyRepository;
    private final RetentionSegmentDefRepository segmentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("📊 Iniciando carga de configuración para CHURN...");
        try {
            seedRetentionStrategies();
            verifyAndSeedRetentionSegments();
            log.info("✅ Configuración de CHURN completada.");
        } catch (Exception e) {
            log.error("❌ Error en ChurnDataSeeder: {}", e.getMessage());
        }
    }

    private void seedRetentionStrategies() {
        if (strategyRepository.count() == 0) {
            List<RetentionStrategyDef> strategies = List.of(
                createStrategy("Descuento en Comisión", "Reducción del 50% en comisión de mantenimiento por 6 meses.", "50.00", "0.35"),
                createStrategy("Oferta Cross-Selling", "Tasa preferencial en préstamo personal o nueva tarjeta.", "20.00", "0.25"),
                createStrategy("Programa VIP Retention", "Acceso a gestor personal y beneficios exclusivos.", "200.00", "0.60")
            );
            strategyRepository.saveAll(strategies);
            log.info("📋 {} Estrategias de retención creadas.", strategies.size());
        }
    }

    private void verifyAndSeedRetentionSegments() {
        if (segmentRepository.count() == 0) {
            log.info("🔍 Verificando datos para creación de segmentos...");

            // Definición de segmentos y sus queries de verificación
            seedSegment("VIPs en Alto Riesgo", 
                        "Balance superior a 50.000 EUR con probabilidad de fuga mayor al 50%",
                        "[{\"op\":\">\",\"val\":50000,\"field\":\"balance\"},{\"op\":\">\",\"val\":0.5,\"field\":\"churn_prob\"}]",
                        "SELECT COUNT(*) FROM public.account_details ad JOIN public.churn_predictions cp ON ad.id_customer = cp.id_customer WHERE ad.balance > 50000 AND cp.churn_probability > 0.5");

            seedSegment("Jóvenes con Pocos Productos", 
                        "Menores de 30 años con menos de 2 productos contratados",
                        "[{\"op\":\"<\",\"val\":30,\"field\":\"age\"},{\"op\":\"<\",\"val\":2,\"field\":\"products\"}]",
                        "SELECT COUNT(*) FROM public.customer cu JOIN public.account_details ad ON cu.id_customer = ad.id_customer WHERE cu.age < 30 AND ad.num_of_products < 2");

            seedSegment("Clientes Mono-Producto con Bajo Balance", 
                        "Un solo producto y balance menor a 5.000 EUR",
                        "[{\"op\":\"<\",\"val\":5000,\"field\":\"balance\"},{\"op\":\"==\",\"val\":1,\"field\":\"products\"}]",
                        "SELECT COUNT(*) FROM public.account_details WHERE balance < 5000 AND num_of_products = 1");

            seedSegment("Perfil Solvente Sin Fidelizar", 
                        "Score crediticio alto con menos de 3 productos contratados",
                        "[{\"op\":\">\",\"val\":650,\"field\":\"score\"},{\"op\":\"<\",\"val\":3,\"field\":\"products\"}]",
                        "SELECT COUNT(*) FROM public.account_details WHERE credit_score > 650 AND num_of_products < 3");

            log.info("📋 Segmentos de retención creados y validados.");
        }
    }

    private void seedSegment(String name, String desc, String rules, String verifySql) {
        Integer count = 0;
        try {
            count = jdbcTemplate.queryForObject(verifySql, Integer.class);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo verificar el segmento '{}': {}. Se creará de todas formas.", name, e.getMessage());
        }

        RetentionSegmentDef seg = new RetentionSegmentDef();
        seg.setName(name);
        seg.setDescription(desc + " (Clientes detectados: " + (count != null ? count : 0) + ")");
        seg.setRulesJson(rules);
        seg.setCreatedAt(LocalDateTime.now());
        segmentRepository.save(seg);
        
        if (count != null && count > 0) {
            log.info("✅ Segmento '{}' creado con {} clientes potenciales.", name, count);
        } else {
            log.info("ℹ️ Segmento '{}' creado (actualmente sin clientes que cumplan el criterio).", name);
        }
    }

    private RetentionStrategyDef createStrategy(String name, String desc, String cost, String impact) {
        RetentionStrategyDef s = new RetentionStrategyDef();
        s.setName(name);
        s.setDescription(desc);
        s.setCostPerClient(new BigDecimal(cost));
        s.setImpactFactor(new BigDecimal(impact));
        s.setIsActive(true);
        s.setCreatedAt(LocalDateTime.now());
        return s;
    }
}
