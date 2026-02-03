package com.naal.bankmind.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuración de WebClient para llamadas a APIs externas
 */
@Configuration
public class WebClientConfig {

    @Value("${api.base-url}")
    private String fraudApiBaseUrl;

    @Value("${api.timeout}")
    private int timeout;

    /**
     * Bean de WebClient configurado para la API de Fraude
     */
    @Bean
    public WebClient fraudApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeout));

        return WebClient.builder()
                .baseUrl(fraudApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Filtro para logging de requests (útil para debugging)
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            System.out.println("===> Request: " + clientRequest.method() + " " + clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    /**
     * Filtro para logging de responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            System.out.println("<=== Response Status: " + clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
