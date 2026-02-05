package com.naal.bankmind.repository.atm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.naal.bankmind.entity.atm.Weather;

public interface WeatherRepository extends JpaRepository<Weather, Short> {

}
