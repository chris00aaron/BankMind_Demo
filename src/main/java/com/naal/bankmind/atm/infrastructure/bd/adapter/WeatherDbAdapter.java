package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.domain.model.Weather;
import com.naal.bankmind.atm.domain.ports.out.repository.WeatherRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaWeatherRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class WeatherDbAdapter implements WeatherRepository {

    private final JpaWeatherRepository jpaWeatherRepository;

    @Override
    public List<Weather> findAll() {
        return jpaWeatherRepository.findAll().stream().
            map(weather -> 
                new Weather(weather.getIdWeather(), 
                            weather.getDescription(), 
                            weather.getImpact())
            ).toList();
    }

    @Override
    public Optional<Weather> findById(@NonNull Short idWeather) {
        return jpaWeatherRepository.findById(idWeather).map(weather -> 
                new Weather(weather.getIdWeather(), 
                            weather.getDescription(), 
                            weather.getImpact())
        );
    }
}
