package com.naal.bankmind.atm.application.dto.response;

import java.util.List;

public record PrediccionDeRetirosDTO(
    List<RetiroEfectivoAtmPrediccionDTO> predicciones,
    List<RetiroHistoricoDTO> retirosHistoricos,
    RetiroEfectivoAtmPrediccionResumenDTO resumen
) {}
