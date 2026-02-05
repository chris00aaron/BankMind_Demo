package com.naal.bankmind.service.atm;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.naal.bankmind.dto.atm.response.WeatherResponseDTO;
import com.naal.bankmind.entity.atm.Weather;
import com.naal.bankmind.repository.atm.WeatherRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class WeatherService {

    private final WeatherRepository weatherRepository;

    public Optional<Weather> buscar(Short id) {
        return weatherRepository.findById(id);
    }

    public List<WeatherResponseDTO> obtenerTiposDeClima() {
        return weatherRepository.findAll().stream()
                .map(weather -> new WeatherResponseDTO(weather.getIdWeather(), weather.getDescription()))
                .collect(Collectors.toList());
    }
}
