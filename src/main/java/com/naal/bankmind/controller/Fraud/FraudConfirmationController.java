package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.service.Fraud.FraudConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller para endpoints públicos de confirmación de fraude
 * (sin autenticación JWT - protegidos con tokens únicos)
 */
@Slf4j
@Controller
@RequestMapping("/api/fraud")
public class FraudConfirmationController {

    private final FraudConfirmationService fraudConfirmationService;

    public FraudConfirmationController(FraudConfirmationService fraudConfirmationService) {
        this.fraudConfirmationService = fraudConfirmationService;
    }

    /**
     * Cliente confirma que la transacción es legítima
     * GET /api/fraud/confirm/{token}
     */
    @GetMapping("/confirm/{token}")
    public String confirmTransaction(@PathVariable String token, Model model) {
        try {
            String message = fraudConfirmationService.confirmLegitimate(token);
            model.addAttribute("success", true);
            model.addAttribute("message", message);
            model.addAttribute("title", "Transacción Confirmada");

            return "confirmation-success";

        } catch (Exception e) {
            log.error("Error al confirmar transacción: {}", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());
            model.addAttribute("title", "Error");

            return "confirmation-success";
        }
    }

    /**
     * Cliente reporta fraude - bloquear tarjeta
     * GET /api/fraud/block/{token}
     */
    @GetMapping("/block/{token}")
    public String blockCard(@PathVariable String token, Model model) {
        try {
            String message = fraudConfirmationService.blockCardAndReject(token);
            model.addAttribute("success", true);
            model.addAttribute("message", message);
            model.addAttribute("title", "Tarjeta Bloqueada");
            model.addAttribute("isBlocked", true);

            return "block-success";

        } catch (Exception e) {
            log.error("Error al bloquear tarjeta: {}", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());
            model.addAttribute("title", "Error");
            model.addAttribute("isBlocked", false);

            return "block-success";
        }
    }
}
