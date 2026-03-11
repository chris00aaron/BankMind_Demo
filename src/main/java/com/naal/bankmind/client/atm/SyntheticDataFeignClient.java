package com.naal.bankmind.client.atm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "synthetic-data-generator-client", url = "http://localhost:8001/retiro-atm")
public interface SyntheticDataFeignClient {

    @PostMapping("/new-data")
    public String generatedNewData(@RequestParam("fecha_objetivo") String fechaObjetivo);
}
