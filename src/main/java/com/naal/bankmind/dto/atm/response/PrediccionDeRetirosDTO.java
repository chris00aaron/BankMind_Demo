package com.naal.bankmind.dto.atm.response;

import java.util.List;

public record PrediccionDeRetirosDTO(
    List<RetiroEfectivoAtmPrediccionDTO> predicciones,
    List<RetiroHistoricoDTO> retirosHistoricos,
    RetiroEfectivoAtmPrediccionResumenDTO resumen
) {}
