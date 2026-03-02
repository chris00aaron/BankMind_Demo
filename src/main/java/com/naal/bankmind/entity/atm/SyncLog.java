package com.naal.bankmind.entity.atm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = { "transactions", "atmStatuses" })
@Entity
@Table(name = "sync_logs", schema = "public")
public class SyncLog {

    public enum SyncStatus {
        IN_PROGRESS, SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sync")
    private Long idSync;

    @Column(name = "sync_start", nullable = false)
    private LocalDateTime syncStart;

    @Column(name = "sync_end")
    private LocalDateTime syncEnd;

    @Column(name = "records_processed")
    private Integer recordsProcessed = 0;

    @Column(name = "records_inserted")
    private Integer recordsInserted = 0;

    @Column(name = "records_updated")
    private Integer recordsUpdated = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status = SyncStatus.IN_PROGRESS;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem = "BANCO_CENTRAL";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "process_log", columnDefinition = "jsonb")
    private List<ProcessLogSync> processLog;

    @JsonManagedReference("syncLog-transactions")
    @OneToMany(mappedBy = "syncLog", fetch = FetchType.LAZY)
    private List<DailyAtmTransaction> transactions = new ArrayList<>();

    @JsonManagedReference("syncLog-atmStatuses")
    @OneToMany(mappedBy = "lastSync", fetch = FetchType.LAZY)
    private List<AtmCurrentStatus> atmStatuses = new ArrayList<>();
}
