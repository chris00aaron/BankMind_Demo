package com.naal.bankmind.atm.infrastructure.mapper;

import java.util.List;

import com.naal.bankmind.atm.domain.model.ProcessLogStep;
import com.naal.bankmind.atm.domain.model.SynchronizationLog;
import com.naal.bankmind.entity.atm.ProcessLogSync;
import com.naal.bankmind.entity.atm.SyncLog;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SyncLogMapper {

    public static SynchronizationLog toDomain(SyncLog entity) {
        return new SynchronizationLog(
                entity.getIdSync(),
                entity.getRecordsInserted(),
                entity.getRecordsProcessed(),
                entity.getRecordsUpdated(),
                entity.getSyncStart(),
                entity.getSyncEnd(),
                entity.getStatus().name(),
                entity.getSourceSystem(),
                entity.getErrorMessage(),
                toProcessLogStep(entity.getProcessLog())
            );
    }

    private static List<ProcessLogStep> toProcessLogStep(List<ProcessLogSync> processLog) {
        if(processLog == null) return null;
        return processLog.stream().map(SyncLogMapper::toProcessLogStep).toList();
    }

    private static ProcessLogStep toProcessLogStep(ProcessLogSync processLog) {
        if(processLog == null) return null;
        return new ProcessLogStep(
                processLog.getAction(),
                processLog.getStatus(),
                processLog.getDetails(),
                processLog.getTimestamp().toString()
            );
    }
}
