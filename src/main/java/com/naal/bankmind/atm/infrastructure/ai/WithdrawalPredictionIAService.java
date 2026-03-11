package com.naal.bankmind.atm.infrastructure.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.ports.out.ai.WithdrawalPredictionIA;
import com.naal.bankmind.client.atm.WithdrawalFeignClient;
import com.naal.bankmind.client.atm.dto.request.InputDataRetiroAtmDTO;
import com.naal.bankmind.client.atm.dto.response.OutputDataRetiroAtmDTO;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@Service
public class WithdrawalPredictionIAService implements WithdrawalPredictionIA {
    private final WithdrawalFeignClient withdrawalFeignClient;

    @Override
    public List<OutputDataPredictionRetiroAtm> predecirWithdrawalHistoric(List<InputDataPredictionRetiroAtm> inputs) {

        // Convertir InputDataPredictionRetiroAtm a InputDataRetiroAtm(Formato aceptado por la api)
        List<InputDataRetiroAtmDTO> inputsDTO = inputs.stream().map(InputDataRetiroAtmDTO::new).toList();

        // Llamar a la api
        List<OutputDataRetiroAtmDTO> outputsDTO = withdrawalFeignClient.predecirWithdrawalHistoric(inputsDTO);

        // Convertir OutputDataRetiroAtm a OutputDataPredictionRetiroAtm
        List<OutputDataPredictionRetiroAtm> outputs = outputsDTO.stream().map(OutputDataRetiroAtmDTO::toOutputDataPredictionRetiroAtm).toList();
        
        return outputs;
    }
}
