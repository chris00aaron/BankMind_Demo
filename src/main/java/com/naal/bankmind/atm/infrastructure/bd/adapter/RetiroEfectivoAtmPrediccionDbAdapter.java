package com.naal.bankmind.atm.infrastructure.bd.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.domain.exception.ModeloPrediccionIndisponibleException;
import com.naal.bankmind.atm.domain.model.ConfidenceInterval;
import com.naal.bankmind.atm.domain.model.ConfidenceModel;
import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.atm.domain.ports.out.repository.ConfidenceModelRepository;
import com.naal.bankmind.atm.domain.ports.out.repository.RetiroEfectivoAtmPrediccionRepository;
import com.naal.bankmind.atm.infrastructure.bd.jpa.JpaDailyWithdrawalPredictionRepository;
import com.naal.bankmind.atm.infrastructure.mapper.DailyWithdrawalPredictionMapper;
import com.naal.bankmind.entity.atm.Atm;
import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;
import com.naal.bankmind.entity.atm.WithdrawalModel;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class RetiroEfectivoAtmPrediccionDbAdapter implements RetiroEfectivoAtmPrediccionRepository {

    private final ConfidenceModelRepository confidenceModelRepository;

    private final JpaDailyWithdrawalPredictionRepository jpaDailyWithdrawalPredictionRepository;
    
    @Override
    public List<RetiroEfectivoAtmPrediccion> obtenerPrediccionesPorFecha(LocalDate fecha) {
        List<DailyWithdrawalPrediction> predicciones = jpaDailyWithdrawalPredictionRepository.findByPredictionDate(fecha);

        //Si retorna vacio, significa que no hay predicciones para la fecha especificada
        if (predicciones.isEmpty())  return Collections.emptyList();
   
        //Posteriormente se usara un mapper para convertir DailyWithdrawalPrediction a RetiroEfectivoAtmPrediccion
        return predicciones.stream().map(
            prediccion -> new RetiroEfectivoAtmPrediccion(
                prediccion.getAtm().getIdAtm(),
                prediccion.getPredictionDate(),
                prediccion.getPredictedValue(),
                prediccion.getLowerBound(),
                prediccion.getUpperBound()
            )
        ).toList();
    }


    @Override
    public List<RetiroEfectivoAtmPrediccion> guardarPredicciones(List<OutputDataPredictionRetiroAtm> nuevasPredicciones) {

        ConfidenceModel confidenceModel = confidenceModelRepository.obtenerConfidenceModelActivo()
            .orElseThrow(() -> new ModeloPrediccionIndisponibleException("No se encontro un modelo de confianza activo"));
        
        List<DailyWithdrawalPrediction> prediccionesGuardadas = new ArrayList<>();

        for (OutputDataPredictionRetiroAtm prediccion : nuevasPredicciones) {
            ConfidenceInterval ci = confidenceModel.calcularIntervaloConfianza(prediccion.retiro());

            //Mapeo de atm
            Atm atm = new Atm();
            atm.setIdAtm(prediccion.atm());

            //Mapeo de modelo
            WithdrawalModel modelo = new WithdrawalModel();
            modelo.setId(confidenceModel.idModel());

            DailyWithdrawalPrediction prediccionGuardada = DailyWithdrawalPrediction.builder()
                    .lowerBound(ci.lowerBound())
                    .predictedValue(prediccion.retiro())
                    .predictionDate(prediccion.predictionDate())
                    .registrationDate(LocalDateTime.now())
                    .upperBound(ci.upperBound())
                    .atm(atm)
                    .withdrawalModel(modelo)
                    .build();
            prediccionesGuardadas.add(prediccionGuardada);
        }

        jpaDailyWithdrawalPredictionRepository.saveAll(prediccionesGuardadas);

        return prediccionesGuardadas.stream().map(DailyWithdrawalPredictionMapper::toRetiroEfectivoAtmPrediccion).toList();
    }
}
