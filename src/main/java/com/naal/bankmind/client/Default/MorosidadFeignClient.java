package com.naal.bankmind.client.Default;

import com.naal.bankmind.dto.Default.Request.MorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Request.BatchMorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;
import com.naal.bankmind.dto.Default.Response.BatchMorosidadResponseDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para comunicarse con la API de predicción de morosidad
 * (Python).
 */
@FeignClient(name = "morosidad-api", url = "${api.base-url}")
public interface MorosidadFeignClient {

    @PostMapping("/morosidad/predict")
    MorosidadResponseDTO predict(@RequestBody MorosidadRequestDTO request);

    @PostMapping("/morosidad/predict/batch")
    BatchMorosidadResponseDTO predictBatch(@RequestBody BatchMorosidadRequestDTO request);

    @PostMapping("/morosidad/refresh-model")
    java.util.Map<String, Object> refreshModel();

    @GetMapping("/morosidad/model-version")
    java.util.Map<String, Object> getModelVersion();
}
