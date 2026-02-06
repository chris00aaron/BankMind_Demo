package com.naal.bankmind.controller.atm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;
import com.naal.bankmind.service.atm.DailyAtmTransactionService;
import com.naal.bankmind.service.atm.DailyWithdrawalPredictionService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
@CrossOrigin(
    origins = "http://localhost:5173", 
    allowedHeaders = "*", 
    allowCredentials = "true",
    methods = {RequestMethod.GET, RequestMethod.POST}
)
@RestController
@RequestMapping("/atm/prediccion")
public class DailyWithdrawalPredictionController {

    private final DailyWithdrawalPredictionService dailyWithdrawalPredictionService;
    private final DailyAtmTransactionService dailyAtmTransactionService;
    
    @GetMapping()
    public ResponseEntity<?> getAllDailyWithdrawalPredictions() {
        LocalDate fecha = LocalDate.of(2025, 12, 2);
        var predicciones = dailyWithdrawalPredictionService.obtenerPrediccionesPorFecha(fecha);
        Map<Long,BigDecimal> balanceoPorAtm = dailyAtmTransactionService.obtenerTransaccionesPorFecha(fecha.minusDays(1)).
            stream().collect(Collectors.toMap((t) -> t.getAtm().getIdAtm(), (t) -> t.getBalanceAfter()));
        
        List<EstadoAtm> estados = predicciones.stream()
                .map((p) -> EstadoAtm.from(p, balanceoPorAtm.get(p.getAtm().getIdAtm())))
                .toList();

        return ResponseEntity.ok(Map.of("estadosAtms", estados));
    }

    public record EstadoAtm(
        String idAtm,
        String direccion,
        String tipoLugar,
        BigDecimal balanceActual,
        BigDecimal porcentaje,
        String estado
    ) {

        public static EstadoAtm from(DailyWithdrawalPrediction prediccion, BigDecimal balanceAnterior) {
            BigDecimal balanceActual = calcularBalanceActual(prediccion.getPredictedValue(), balanceAnterior);
            BigDecimal porcentaje = balanceActual.divide(prediccion.getAtm().getMaxCapacity(), 2, RoundingMode.HALF_UP);
            String estado = establecerEstado(porcentaje);

            return new EstadoAtm(
                getFormatearId(prediccion.getAtm().getIdAtm()),
                prediccion.getAtm().getAddress(),
                prediccion.getAtm().getLocationType().getDescription(),
                balanceActual,
                porcentaje,
                estado
            );
        }

        private static String getFormatearId(Long idAtm) {
            StringBuilder sb = new StringBuilder("ATM-");
            sb.append(String.format("%04d", idAtm));
            return sb.toString();
        }

        private static BigDecimal calcularBalanceActual(BigDecimal retiroPredicho, BigDecimal balanceAnterior) {
            BigDecimal balanceActual = balanceAnterior.subtract(retiroPredicho);
            return balanceActual.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balanceActual;
        }

        private static String establecerEstado(BigDecimal porcentaje) {
            // 0.15 = 15% // Critico
            // 0.40 = 50% // Alerta
            // 0.75 = 75% // Normal
            if (porcentaje.compareTo(BigDecimal.valueOf(0.15)) < 0) {
                return "CRITICO";
            } else if (porcentaje.compareTo(BigDecimal.valueOf(0.50)) < 0) {
                return "ALERTA";
            } else {
                return "NORMAL";
            }
        }
    }
}
