package com.naal.bankmind.client.atm;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.naal.bankmind.client.atm.dto.request.InputDataRetiroAtmDTO;
import com.naal.bankmind.client.atm.dto.response.OutputDataRetiroAtmDTO;


@FeignClient(name = "withdrawal-client", url = "http://localhost:8000/api/atm")
public interface WithdrawalFeignClient {

    @PostMapping("/v1/withdrawal/off-time")
    public List<OutputDataRetiroAtmDTO> predecirWithdrawalHistoric(@RequestBody List<InputDataRetiroAtmDTO> inputDataRetiroAtms);
}
