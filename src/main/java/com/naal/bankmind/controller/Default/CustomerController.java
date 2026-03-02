package com.naal.bankmind.controller.Default;

import com.naal.bankmind.dto.Default.Request.BatchFilterRequestDTO;
import com.naal.bankmind.dto.Default.Response.AccountSummaryDTO;
import com.naal.bankmind.dto.Default.Response.CustomerSearchResponseDTO;
import com.naal.bankmind.service.Default.CustomerService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para operaciones de clientes.
 */
@RestController
@RequestMapping("/api/morosidad/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Busca clientes por nombre o ID.
     */
    @GetMapping("/search")
    public ResponseEntity<List<CustomerSearchResponseDTO>> searchCustomers(
            @RequestParam(name = "q", required = true) String q) {

        if (q == null || q.length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        List<CustomerSearchResponseDTO> results = customerService.searchCustomers(q);
        return ResponseEntity.ok(results);
    }

    /**
     * Obtiene las cuentas de un cliente específico.
     */
    @GetMapping("/{customerId}/accounts")
    public ResponseEntity<List<AccountSummaryDTO>> getCustomerAccounts(
            @PathVariable Long customerId) {

        List<AccountSummaryDTO> accounts = customerService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Filtra clientes según criterios y retorna lista de recordIds.
     * Usado para predicción batch.
     */
    @PostMapping("/filter")
    public ResponseEntity<List<Long>> filterCustomers(@RequestBody BatchFilterRequestDTO filters) {
        List<Long> recordIds = customerService.filterCustomersGetRecordIds(filters);
        return ResponseEntity.ok(recordIds);
    }
}
