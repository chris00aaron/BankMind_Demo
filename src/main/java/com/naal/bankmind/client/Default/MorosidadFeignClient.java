package com.naal.bankmind.client.Default;

import com.naal.bankmind.dto.Default.Request.MorosidadRequestDTO;
import com.naal.bankmind.dto.Default.Response.MorosidadResponseDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para comunicarse con la API de predicción de morosidad
 * (Python).
 */
@FeignClient(name = "morosidad-api", url = "${ai.morosidad.url}")
public interface MorosidadFeignClient {

    @PostMapping("/morosidad/predict")
    MorosidadResponseDTO predict(@RequestBody MorosidadRequestDTO request);
}
