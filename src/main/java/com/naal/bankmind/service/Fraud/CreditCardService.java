package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.entity.CreditCards;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de tarjetas de crédito
 */
@Slf4j
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;

    public CreditCardService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    /**
     * Bloquear una tarjeta de crédito
     */
    @Transactional
    public void blockCard(Long ccNum) {
        CreditCards card = creditCardRepository.findById(ccNum)
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada: " + ccNum));

        card.setIsActive(false);
        creditCardRepository.save(card);

        log.info("Tarjeta bloqueada: {}", card.getMaskedCardNumber());
    }

    /**
     * Desbloquear una tarjeta de crédito
     */
    @Transactional
    public void unblockCard(Long ccNum) {
        CreditCards card = creditCardRepository.findById(ccNum)
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada: " + ccNum));

        card.setIsActive(true);
        creditCardRepository.save(card);

        log.info("Tarjeta desbloqueada: {}", card.getMaskedCardNumber());
    }

    /**
     * Obtener información de una tarjeta
     */
    public CreditCards getCard(Long ccNum) {
        return creditCardRepository.findById(ccNum)
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada: " + ccNum));
    }
}
