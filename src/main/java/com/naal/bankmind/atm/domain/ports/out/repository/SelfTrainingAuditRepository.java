package com.naal.bankmind.atm.domain.ports.out.repository;

import com.naal.bankmind.atm.domain.criteria_query.SelfTrainingAuditCriteria;
import com.naal.bankmind.atm.domain.model.PageResult;
import com.naal.bankmind.atm.domain.model.SelfTrainingAudit;

public interface SelfTrainingAuditRepository {
    /**
     * @param page El número de página. 
     * @param size El tamaño de la pagina
     * @param criteria Filtros para el filtrado de los resultados
     * @return Pagina con Auditorias de los procesos de autoentrenamiento
     */
    PageResult<SelfTrainingAudit> buscarHistorial(int page, int size, SelfTrainingAuditCriteria criteria);
}
