package com.naal.bankmind.scheduler;

import com.naal.bankmind.service.Login.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanExpiredOTPs {

    private final OtpService otpService;

    /**
     * Limpia los códigos OTP cada 3 horas
     */
    @Scheduled(cron = "0 0/180 * * * ?")
    public void cleanupExpiredOtps() {
        log.info("\n---------------------------------------- Limpiando OTPs expirados ----------------------------------------");
        otpService.cleanupExpiredOtps();
        log.info("----------------------------------------------------------------------------------------------------------\n");
    }
}
