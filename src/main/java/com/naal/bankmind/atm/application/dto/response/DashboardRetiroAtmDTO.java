package com.naal.bankmind.atm.application.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardRetiroAtmDTO(
        List<RetiroEfectivoAtmPrediccionDTO> retirosPredichos,
        RetiroEfectivoAtmPrediccionResumenDTO resumenRetiroEfectivoAtm,
        ResumenOperativoAtmDTO resumenOperativoAtms,
        List<RetiroHistoricoDTO> retirosHistoricos,
        SegmentacionRetiroDTO segmentacionRetiro,
        Long atmsConPotencialDeFaltaStock,
        Map<String, Object> featuresImportancia
) {}
