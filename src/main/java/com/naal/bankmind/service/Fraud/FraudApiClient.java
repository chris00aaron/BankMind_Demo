package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.BatchApiRequestDto;
import com.naal.bankmind.dto.Fraud.BatchApiResponseDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionRequestDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Cliente HTTP para comunicarse con la API de Fraude (Python/FastAPI)
 * Usa WebClient (reactivo, no bloqueante)
 */
@Service
public class FraudApiClient {

    private final WebClient webClient;

    public FraudApiClient(@Qualifier("fraudApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Llama al endpoint de predicción de fraude en la API de Python (una
     * transacción)
     * 
     * @param request Datos de la transacción a evaluar
     * @return Respuesta con veredicto y factores de riesgo
     * @throws RuntimeException si hay error de comunicación con la API
     */
    public FraudPredictionResponseDto predictFraud(FraudPredictionRequestDto request) {
        try {
            return webClient.post()
                    .uri("/api/v1/fraud/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FraudPredictionResponseDto.class)
                    .block(); // Bloqueamos porque estamos en contexto síncrono (Spring MVC)
        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "Error al llamar API de Fraude: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error de conexión con API de Fraude: " + e.getMessage(), e);
        }
    }

    /**
     * Llama al endpoint de predicción por lotes (múltiples transacciones en una
     * sola llamada HTTP)
     * Más eficiente que llamar predictFraud() múltiples veces.
     * 
     * @param requests Lista de transacciones a evaluar
     * @return Respuesta con resultados de todas las transacciones
     * @throws RuntimeException si hay error de comunicación con la API
     */
    public BatchApiResponseDto predictFraudBatch(List<FraudPredictionRequestDto> requests) {
        try {
            BatchApiRequestDto batchRequest = BatchApiRequestDto.builder()
                    .transactions(requests)
                    .build();

            return webClient.post()
                    .uri("/api/v1/fraud/predict-batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(batchRequest)
                    .retrieve()
                    .bodyToMono(BatchApiResponseDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                    "Error al llamar API de Fraude (batch): " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                    e);
        } catch (Exception e) {
            throw new RuntimeException("Error de conexión con API de Fraude (batch): " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si la API de Fraude está disponible
     */
    public boolean isApiAvailable() {
        try {
            webClient.get()
                    .uri("/vivo")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
