package com.naal.bankmind.atm.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.mapper.PromedioRetiroHistoricoMapper;
import com.naal.bankmind.atm.domain.exception.ModeloPrediccionIndisponibleException;
import com.naal.bankmind.atm.domain.exception.WeatherNotFoundException;
import com.naal.bankmind.atm.domain.model.ConfidenceInterval;
import com.naal.bankmind.atm.domain.model.ConfidenceModel;
import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.PromedioAtmFeature;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.atm.domain.model.Weather;
import com.naal.bankmind.atm.domain.ports.in.GenerarPrediccionSimuladaUseCase;
import com.naal.bankmind.atm.domain.ports.in.RealizarPrediccionUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.ConfidenceModelRepository;
import com.naal.bankmind.atm.domain.ports.out.repository.PromedioAtmFeatureRepository;
import com.naal.bankmind.atm.domain.ports.out.repository.WeatherRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class GenerarPrediccionSimuladaServicio implements GenerarPrediccionSimuladaUseCase {

    private final PromedioAtmFeatureRepository promedioAtmFeatureRepository;
    private final WeatherRepository weatherRepository;
    private final ConfidenceModelRepository confidenceModelRepository;

    private final RealizarPrediccionUseCase realizarPrediccionUseCase;
 
    @Override
    public List<RetiroEfectivoAtmPrediccion> generarPrediccion(LocalDate fecha, Short idWeather) {
        Short dia = (short) fecha.getDayOfMonth();
        Short mes = (short) fecha.getMonthValue();

        //Obtener datos historicos de la bd
        Weather weather = weatherRepository.findById(idWeather).orElseThrow(() -> new WeatherNotFoundException(idWeather));
        List<PromedioAtmFeature> promedioRetiroHistorico = promedioAtmFeatureRepository.obtenerPromediosAtmFeatures(dia, mes);
        
        //Preparar datos para realizar la consulta a la prediccion
        List<InputDataPredictionRetiroAtm> inputData = promedioRetiroHistorico.stream().map(p -> PromedioRetiroHistoricoMapper.toInputDataRetiroAtm(p, fecha, weather.impact())).toList();
        List<OutputDataPredictionRetiroAtm> predicciones = realizarPrediccionUseCase.generarPrediccion(inputData);

        return calcularIntervaloConfianza(predicciones);
    }


    private List<RetiroEfectivoAtmPrediccion> calcularIntervaloConfianza(List<OutputDataPredictionRetiroAtm> predicciones) {
        ConfidenceModel confidenceModel = confidenceModelRepository.obtenerConfidenceModelActivo()
            .orElseThrow(() -> new ModeloPrediccionIndisponibleException("No se encontro un modelo de confianza activo"));

        return predicciones.stream().map(p -> {
            ConfidenceInterval ci = confidenceModel.calcularIntervaloConfianza(p.retiro());
            return new RetiroEfectivoAtmPrediccion(
                p.atm(),
                p.predictionDate(),
                p.retiro(),
                ci.lowerBound(),
                ci.upperBound()
            );
        }).toList();
    }

}
