package com.naal.bankmind.atm.domain.ports.out.ai;

import java.util.List;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;

public interface WithdrawalPredictionIA {

    List<OutputDataPredictionRetiroAtm> predecirWithdrawalHistoric(List<InputDataPredictionRetiroAtm> inputs);

}
