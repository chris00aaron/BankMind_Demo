package com.naal.bankmind.controller.atm;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naal.bankmind.atm.domain.model.ATMFeatureModel;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.ports.out.repository.ATMFeatureModelRepository;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/atm/feature")
@AllArgsConstructor
public class AtmFeatureController {

    private final ATMFeatureModelRepository atmFeatureRepository;

    @GetMapping
    public ApiResponse<PageResult<ATMFeatureModel>> findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {

        return ApiResponse.success("Se encontraron exitosamente las caracteristicas del cajero", 
        atmFeatureRepository.findAll(page, size));
    }
}
