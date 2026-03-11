package com.naal.bankmind.atm.application.mapper;

import com.naal.bankmind.atm.application.dto.response.WeatherDTO;
import com.naal.bankmind.atm.domain.model.Weather;

public class WeatherMapper {

    public static WeatherDTO toWeatherDTO(Weather weather) {
        return new WeatherDTO(weather.id(), weather.name());
    }
}
