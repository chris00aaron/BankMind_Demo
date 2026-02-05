package com.naal.bankmind.service.atm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.naal.bankmind.dto.atm.response.ResumeOperativoDTO;
import com.naal.bankmind.dto.atm.response.RetiroEfectivoAtmPrediccionDTO;
import com.naal.bankmind.entity.atm.Atm;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;
import com.naal.bankmind.entity.atm.TransactionType;
import com.naal.bankmind.repository.atm.AtmRepository;
import com.naal.bankmind.repository.atm.DailyAtmTransactionRepository;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@AllArgsConstructor
public class AtmService {

    private final AtmRepository atmRepository;
    private final DailyAtmTransactionRepository dailyAtmTransactionRepository;

    public ResumeOperativoDTO obtenerResumenOperatividad() {
        List<Atm> atms = atmRepository.findAll();

        //Contabilizar cuantos estan actiuvos e incaticos 
        long activos = atms.stream().filter(Atm::isActive).count();
        long inactivos = Math.abs(activos - atms.size());

        return new ResumeOperativoDTO(activos, inactivos);
    }

    public long contabilizarAtmsConPotencialDeFaltaStock(LocalDate fecha, List<RetiroEfectivoAtmPrediccionDTO> predicciones) {
        List<DailyAtmTransaction> transacciones = dailyAtmTransactionRepository.findTransaccionesDelDiaEnAtmsActivos(fecha, TransactionType.WITHDRAWAL);

        Map<Long, BigDecimal> prediccionesMap = predicciones.stream()
                .collect(Collectors.toMap(RetiroEfectivoAtmPrediccionDTO::idAtm, RetiroEfectivoAtmPrediccionDTO::retiroPrevisto));
        
        Long atmsConPotencialDeFaltaStock = 0L;

        for (DailyAtmTransaction transaccion : transacciones) {
            BigDecimal predicción = prediccionesMap.get(transaccion.getAtm().getIdAtm());
            BigDecimal balanceAnterior = transaccion.getBalanceAfter();
            BigDecimal balanceDespues = balanceAnterior.subtract(predicción);
            
            if(balanceDespues.compareTo(BigDecimal.ZERO) <= 0) {
                atmsConPotencialDeFaltaStock++;
            }
        }

        return atmsConPotencialDeFaltaStock;
    }
}
