package com.naal.bankmind.controller.atm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.SynchronizationLog;
import com.naal.bankmind.atm.domain.ports.out.repository.SynchronizationLogRepository;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.GET}
)
@RestController
@RequestMapping("/atm/sync")
public class SynchronizationLogController {

    private final SynchronizationLogRepository synchronizationLogRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<SynchronizationLog>>> listarPaginado(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {   
        var result = synchronizationLogRepository.listarPaginado(page, size);
        return ResponseEntity.ok(ApiResponse.success("Logs de sincronización obtenidos correctamente", result));
    }

}
