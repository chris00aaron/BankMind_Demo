package com.naal.bankmind.atm.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.WeatherDTO;
import com.naal.bankmind.atm.application.mapper.WeatherMapper;
import com.naal.bankmind.atm.domain.ports.in.ListarClimasDisponiblesUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.WeatherRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ListarClimasDisponiblesService implements ListarClimasDisponiblesUseCase {

    private final WeatherRepository weatherRepository;

    @Override
    public List<WeatherDTO> listarClimasDisponibles() {
        return weatherRepository.findAll().stream().map(WeatherMapper::toWeatherDTO).toList();
    }
}