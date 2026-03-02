package com.naal.bankmind.service.atm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.naal.bankmind.client.atm.WithdrawalFeignClient;
import com.naal.bankmind.dto.atm.projection.AtmAvgProjectionDTO;
import com.naal.bankmind.dto.atm.projection.WithdrawalAvgProjectionDTO;
import com.naal.bankmind.dto.atm.request.RetiroEfectivoAtmBasadoEnHistoricoRequestDTO;
import com.naal.bankmind.dto.atm.response.PrediccionDeRetirosDTO;
import com.naal.bankmind.dto.atm.response.RetiroEfectivoAtmPrediccionDTO;
import com.naal.bankmind.dto.atm.response.RetiroEfectivoAtmPrediccionResumenDTO;
import com.naal.bankmind.dto.atm.response.RetiroHistoricoDTO;
import com.naal.bankmind.entity.atm.Weather;
import com.naal.bankmind.repository.atm.AtmFeaturesRepository;
import com.naal.bankmind.utils.atm.ConfidenceInterval;
import com.naal.bankmind.utils.atm.ModelConfidenceService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@Service
public class AtmFeaturesService {

    //Cliente para llamar al modelo de predicción
    private final WithdrawalFeignClient withdrawalFeignClient;

    //Servicio 
    private final WeatherService weatherService;
    private final DailyAtmTransactionService dailyAtmTransactionService;
    private final ModelConfidenceService modelConfidenceService;

    //Repositorio
    private final AtmFeaturesRepository atmFeaturesRepository;

    /**
     * Este método se encarga de predecir el retiro de efectivo en cajeros automáticos
     * basado en el histórico de retiros.
     * 
     * @param requestDTO DTO con la información del retiro de efectivo en cajeros automáticos
     * @return DTO con la predicción de retiros de efectivo en cajeros automáticos 
     *         (retiros previstos + Intcervalo de Confianza + retiros históricos)
     */
    public PrediccionDeRetirosDTO predecirBasadoEnHistorico(
            RetiroEfectivoAtmBasadoEnHistoricoRequestDTO requestDTO) {
        List<AtmAvgProjectionDTO> atmFeaturesAvg = atmFeaturesRepository
                .obtenerPromedios(requestDTO.diaDelMesSolicitado(), requestDTO.mesSolicitado());
        Weather weather = weatherService.buscar(requestDTO.idWeather()).orElseThrow();

        /** 
         * List<InputDataRetiroAtm> inputList = 
        atmFeaturesAvg.stream()
                .map(p -> {
                    InputDataRetiroAtm dto = new InputDataRetiroAtm();

                    dto.setAtm(p.getIdAtm());
                    dto.setLag1(p.getAvgLag1());
                    dto.setLag5(p.getAvgLag5());
                    dto.setLag11(p.getAvgLag11());
                    dto.setTendencia_lags(p.getAvgTendenciaLags());
                    dto.setRatio_finde_vs_semana(p.getAvgRatioFindeVsSemana());
                    dto.setRetiros_finde_anterior(p.getAvgRetirosFindeAnterior());
                    dto.setRetiros_domingo_anterior(p.getAvgRetirosDomingoAnterior());

                    dto.setDiaSemana(requestDTO.diaDeLaSemanaSolicitado());
                    dto.setCaida_reciente(p.getAvgCaidaReciente());
                    dto.setDomingo_bajo(p.getAvgDomingoBajo());
                    dto.setUbicacion(p.getLocationType());
                    dto.setAmbiente(weather.getImpact());

                    return dto;
                })
                .collect(Collectors.toList());

        List<OutputDataRetiroAtm> outputList = withdrawalFeignClient.predecirWithdrawalHistoric(inputList);

        List<RetiroEfectivoAtmPrediccionDTO> outputListWithCI = outputList.stream()
                .map(out -> {
                    ConfidenceInterval ci = modelConfidenceService.calcularIntervaloConfianza(out.retiro());
                    RetiroEfectivoAtmPrediccionDTO prediccion = new RetiroEfectivoAtmPrediccionDTO(out.atm(),
                            out.retiro().setScale(2, RoundingMode.HALF_UP),
                            ci.lowerBound().setScale(2, RoundingMode.HALF_UP),
                            ci.upperBound().setScale(2, RoundingMode.HALF_UP),
                            ci.confidenceLevel());
                    return prediccion;
                })
                .collect(Collectors.toList());

        BigDecimal totalPrevisto = BigDecimal.ZERO;
        BigDecimal totalOptimista = BigDecimal.ZERO;
        BigDecimal totalPesimista = BigDecimal.ZERO;

        for (RetiroEfectivoAtmPrediccionDTO dto : outputListWithCI) {
            totalPrevisto   = totalPrevisto.add(dto.retiroPrevisto());
            totalOptimista  = totalOptimista.add(dto.upperBound());
            totalPesimista  = totalPesimista.add(dto.lowerBound());
        }

        RetiroEfectivoAtmPrediccionResumenDTO resumen = new 
                RetiroEfectivoAtmPrediccionResumenDTO(totalPrevisto, totalOptimista, totalPesimista);      

        Map<Long, BigDecimal> retirosPrevistoPorAtm = outputList.stream()
                .collect(Collectors.toMap(OutputDataRetiroAtm::atm, OutputDataRetiroAtm::retiro));

        List<WithdrawalAvgProjectionDTO> withdrawalAvgProjectionDTOs = dailyAtmTransactionService
                .obtenerRetirosHistoricos(requestDTO.diaDelMesSolicitado(), requestDTO.mesSolicitado());

        List<RetiroHistoricoDTO> retiroHistoricoDTOs = withdrawalAvgProjectionDTOs.stream()
                .map(withdrawalAvg -> {
                    return new RetiroHistoricoDTO(withdrawalAvg.getIdAtm(),
                            withdrawalAvg.getAvgWithdrawal().setScale(2, RoundingMode.HALF_UP), 
                            retirosPrevistoPorAtm.get(withdrawalAvg.getIdAtm()).setScale(2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());


        return new PrediccionDeRetirosDTO(outputListWithCI, retiroHistoricoDTOs, resumen);
        */
       
        return null;
    }

    public List<WithdrawalAvgProjectionDTO> predecirBasadoEnHistorico(Short diaDelMes, Short mes) {
        return dailyAtmTransactionService.obtenerRetirosHistoricos(diaDelMes, mes);
    }

    public List<RetiroHistoricoDTO> predecirBasadoEnHistoricoComparadoConPrediccion(Short diaDelMes, Short mes, List<RetiroEfectivoAtmPrediccionDTO> prediccionDeRetirosDTO) {
        log.info("Dia del mes: {}", diaDelMes);
        log.info("Mes: {}", mes);
        log.info("Prediccion de retiros: {}", prediccionDeRetirosDTO);
        
        List<WithdrawalAvgProjectionDTO> datosHistoricos = this.predecirBasadoEnHistorico(diaDelMes, mes);
        log.info("Datos historicos: {}", datosHistoricos);
        
        Map<Long, BigDecimal> retirosPrevistoPorAtm = prediccionDeRetirosDTO.stream()
                .collect(Collectors.toMap(RetiroEfectivoAtmPrediccionDTO::idAtm, RetiroEfectivoAtmPrediccionDTO::retiroPrevisto));
    
        return datosHistoricos.stream()
                .filter(dh -> retirosPrevistoPorAtm.containsKey(dh.getIdAtm()))
                .map(dh -> new RetiroHistoricoDTO(
                        dh.getIdAtm(),
                        dh.getAvgWithdrawal(), 
                        retirosPrevistoPorAtm.get(dh.getIdAtm())
                    )
                )
                .collect(Collectors.toList());
    }
}