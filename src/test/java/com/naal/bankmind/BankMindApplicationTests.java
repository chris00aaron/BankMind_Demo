package com.naal.bankmind;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaDailyAtmTransactionRepository;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootTest
class BankMindApplicationTests {

	@Autowired
	private JpaDailyAtmTransactionRepository jpaDailyAtmTransactionRepository;

	@Test
	void contextLoads() {
		var result = jpaDailyAtmTransactionRepository.obtenerResumenTransacciones(LocalDate.of(2025, 11, 1), LocalDate.of(2022, 11, 15));
		result.forEach(t -> log.info("Log: {}", t));
	}
}
