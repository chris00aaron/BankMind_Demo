package com.naal.bankmind.atm.domain.exception;

public class WeatherNotFoundException extends RuntimeException {
    
    public WeatherNotFoundException(Short idWeather) {
        super("No se encontro el clima con id: " + idWeather);
    }
}
