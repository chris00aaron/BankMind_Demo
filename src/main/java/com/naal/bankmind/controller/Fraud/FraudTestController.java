package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.scheduler.FraudTrainingScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud/test")
public class FraudTestController {

    @Autowired
    private FraudTrainingScheduler scheduler;

    @PostMapping("/trigger-training")
    public ResponseEntity<String> triggerTraining() {
        // Ejecutar en hilo separado para no bloquear la respuesta HTTP
        new Thread(() -> scheduler.runWeeklyTraining()).start();
        return ResponseEntity.ok("Training triggered in background. Check logs for progress.");
    }
}
