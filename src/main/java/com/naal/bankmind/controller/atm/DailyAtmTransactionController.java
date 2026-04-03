package com.naal.bankmind.controller.atm;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.application.dto.response.ResumenTransaccionDTO;
import com.naal.bankmind.atm.application.mapper.TransactionSummaryMapper;
import com.naal.bankmind.atm.domain.ports.out.repository.TransactionSummaryRepository;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@RequestMapping("/api/atm/transaction")
@RestController
public class DailyAtmTransactionController {

    private final TransactionSummaryRepository transactionSummaryRepository;


    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ResumenTransaccionDTO>> obtenerResumenTransacciones(
        @RequestParam LocalDate desde,
        @RequestParam LocalDate hasta
    ) {   
        var result = transactionSummaryRepository.obtenerResumenTransacciones(desde, hasta);
        var resumen = result.stream().map(TransactionSummaryMapper::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(
            "Resumen de transacciones obtenido correctamente",
            new ResumenTransaccionDTO(resumen)
        ));
    }

}
