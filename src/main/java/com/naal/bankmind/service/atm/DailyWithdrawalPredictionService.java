package com.naal.bankmind.service.atm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.naal.bankmind.client.atm.WithdrawalFeignClient;
import com.naal.bankmind.client.atm.dto.request.InputDataRetiroAtm;
import com.naal.bankmind.client.atm.dto.response.OutputDataRetiroAtm;
import com.naal.bankmind.entity.atm.Atm;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;
import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;
import com.naal.bankmind.entity.atm.WithdrawalModel;
import com.naal.bankmind.mapper.atm.DailyAtmTransactionMapper;
import com.naal.bankmind.repository.atm.DailyWithdrawalPredictionRepository;
import com.naal.bankmind.utils.atm.ConfidenceInterval;
import com.naal.bankmind.utils.atm.ModelConfidenceService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@Service
public class DailyWithdrawalPredictionService {

    //Cliente para llamar al modelo de predicción
    private final WithdrawalFeignClient withdrawalFeignClient;
    
    //Servicio 
    private final DailyAtmTransactionService dailyAtmTransactionService;
    private final ModelConfidenceService modelConfidenceService;
    private final WithdrawalModelService withdrawalModelService;

    //Repositorio
    private final DailyWithdrawalPredictionRepository dailyWithdrawalPredictionRepository;


    /**
     * Este método se encarga de obtener las predicciones de retiros de efectivo en cajeros automáticos
     * para una fecha específica, en caso no existan predicciones para la fecha especificada, se generan
     * 
     * @param fecha La fecha para la cual se quieren obtener las predicciones
     * @return Una lista de DailyWithdrawalPrediction que contiene las predicciones de retiros de efectivo
     *         para la fecha especificada
     */
    public List<DailyWithdrawalPrediction> obtenerPrediccionesPorFecha(LocalDate fecha) {
        List<DailyWithdrawalPrediction> predicciones = dailyWithdrawalPredictionRepository.findByPredictionDate(fecha);

        if(!predicciones.isEmpty()) {
            log.info("Se encontraron predicciones para la fecha: {}", fecha);
            return predicciones;
        }

        log.info("No se encontraron predicciones para la fecha: {}. Generando nuevas...", fecha);

        LocalDate fechaAyer = fecha.minusDays(1);
        List<DailyAtmTransaction> transaccionesAyer = dailyAtmTransactionService.obtenerTransaccionesPorFecha(fechaAyer);
        
        // El .stream().toList() es inmutable y seguro contra ConcurrentModificationException
        List<InputDataRetiroAtm> inputDataList = transaccionesAyer.stream()
                .map(DailyAtmTransactionMapper::toAtmFeatures)
                .toList();
        
        // AQUÍ DEBERÍAS LLAMAR A TU MODELO DE ML O AL SERVICIO QUE GENERA LA PREDICCIÓN
        List<OutputDataRetiroAtm> nuevasPredicciones = withdrawalFeignClient.predecirWithdrawalHistoric(inputDataList);

        // Mapear los id_atm con los atm
        Map<Long, Atm> atmMap = new HashMap<>();

        for (DailyAtmTransaction transaccion : transaccionesAyer) {
            atmMap.put(transaccion.getAtm().getIdAtm(), transaccion.getAtm());
        }

        //Obtener el modelo de retiro actual en produccion
        WithdrawalModel modelo = withdrawalModelService.obtenerModeloActual().
            orElseThrow(() -> new RuntimeException("No se encontro el modelo de retiro actual"));
    
        List<DailyWithdrawalPrediction> prediccionesGuardadas = new ArrayList<>();

        for (OutputDataRetiroAtm prediccion : nuevasPredicciones) {
            ConfidenceInterval ci = modelConfidenceService.calcularIntervaloConfianza(prediccion.retiro());

            DailyWithdrawalPrediction prediccionGuardada = DailyWithdrawalPrediction.builder()
                    .lowerBound(ci.lowerBound())
                    .predictedValue(prediccion.retiro())
                    .predictionDate(fecha)
                    .registrationDate(LocalDateTime.now())
                    .upperBound(ci.upperBound())
                    .atm(atmMap.get(prediccion.atm()))
                    .withdrawalModel(modelo)
                    .build();
            prediccionesGuardadas.add(prediccionGuardada);
        }

        dailyWithdrawalPredictionRepository.saveAll(prediccionesGuardadas);
        log.info("Se guardaron {} predicciones para la fecha: {}", prediccionesGuardadas.size(), fecha);

        return prediccionesGuardadas;
    }
}
