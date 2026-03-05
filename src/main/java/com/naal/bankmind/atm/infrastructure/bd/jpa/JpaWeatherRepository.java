package com.naal.bankmind.atm.infrastructure.bd.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.naal.bankmind.entity.atm.Weather;

public interface JpaWeatherRepository extends JpaRepository<Weather, Short> {

}
