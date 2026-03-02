package com.naal.bankmind.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuración de clientes HTTP para llamadas a APIs externas.
 *
 * Beans provistos:
 * - {@code fraudApiWebClient} — WebClient reactivo para la API de predicción
 * (FastAPI).
 * - {@code schedulerRestTemplate} — RestTemplate síncrono con timeout largo
 * para el Scheduler
 * (el entrenamiento puede tardar hasta 15 minutos).
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${api.base-url}")
    private String fraudApiBaseUrl;

    @Value("${api.timeout}")
    private int apiTimeoutMs;

    @Value("${fraud.scheduler.timeout-ms}")
    private int schedulerTimeoutMs;

    /**
     * WebClient configurado para la API de Fraude (predicción / batch / reload).
     */
    @Bean
    public WebClient fraudApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(apiTimeoutMs));

        return WebClient.builder()
                .baseUrl(fraudApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * RestTemplate síncrono para el
     * {@link com.naal.bankmind.scheduler.FraudTrainingScheduler}.
     * Timeout configurado en {@code fraud.scheduler.timeout-ms} (900 000 ms por
     * defecto)
     * para no caducar durante un entrenamiento largo con Optuna.
     */
    @Bean
    public RestTemplate schedulerRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofMillis(schedulerTimeoutMs))
                .build();
    }

    // ─── Filtros de logging para WebClient ───────────────────────────────────

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.debug("===> Request: {} {}", req.method(), req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            log.debug("<=== Response Status: {}", res.statusCode());
            return Mono.just(res);
        });
    }
}
