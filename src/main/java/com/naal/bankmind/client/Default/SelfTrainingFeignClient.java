package com.naal.bankmind.client.Default;

import com.naal.bankmind.dto.Default.Request.TrainingRequestDTO;
import com.naal.bankmind.dto.Default.Response.TrainingResponseDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para comunicarse con la API de Auto-Entrenamiento (Python).
 */
@FeignClient(name = "self-training-api", url = "${self-training.api.base-url}")
public interface SelfTrainingFeignClient {

    @PostMapping("/morosidad/train")
    TrainingResponseDTO triggerTraining(@RequestBody TrainingRequestDTO request);
}
