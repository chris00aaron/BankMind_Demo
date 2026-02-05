package com.naal.bankmind.controller.atm;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.dto.atm.response.WeatherResponseDTO;
import com.naal.bankmind.service.atm.WeatherService;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.GET, RequestMethod.POST}
)
@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WeatherResponseDTO>>> obtenerTiposDeClima() {
        return ResponseEntity.ok(ApiResponse.success("Tipos de clima obtenidos correctamente", weatherService.obtenerTiposDeClima()));
    }
}
