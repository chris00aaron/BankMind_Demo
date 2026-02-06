package com.naal.bankmind.utils.atm;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.naal.bankmind.service.atm.WithdrawalModelService;

import jakarta.annotation.PostConstruct;

@Service
public class ModelConfidenceService {

    @Autowired
    private WithdrawalModelService withdrawalModelService;

    private volatile ConfidenceModel confidenceModel;

    //@PostConstruct
    public void loadModel() {
        inicializar();
    }

    public synchronized void inicializar() {
        ConfidenceModel model = withdrawalModelService
                .obtenerConfidenceModelActivo()
                .orElseThrow(() -> new IllegalStateException("No se encontró modelo activo"));

        this.confidenceModel = model;
    }

    public ConfidenceInterval calcularIntervaloConfianza(BigDecimal prediccion) {
        ConfidenceModel model = confidenceModel;
        if (model == null) {
            throw new IllegalStateException("Modelo no inicializado");
        }

        BigDecimal lower = prediccion.subtract(model.margin());
        BigDecimal upper = prediccion.add(model.margin());

        return new ConfidenceInterval(lower, upper, model.confidenceLevel());
    }

    public Map<String, Object> mostrarImportanciaFeatures() {
        ConfidenceModel model = confidenceModel;
        if (model == null) {
            throw new IllegalStateException("Modelo no inicializado");
        }
        return model.importancesFeatures();
    }

    public synchronized void reset() {
        this.confidenceModel = null;
    }
}
