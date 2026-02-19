package com.naal.bankmind.entity.Default;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.naal.bankmind.entity.Default.POJO.MetricsResults;
import com.naal.bankmind.entity.Default.POJO.ParametersOptuna;
import java.util.Map;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "training_history")
public class TrainingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_training_history")
    private Long idTrainingHistory;

    @Column(name = "best_cadidate_model", length = 50)
    private String bestCadidateModel;

    @Column(name = "training_date")
    private LocalDateTime trainingDate;

    @Column(name = "in_production")
    private Boolean inProduction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters_optuna", columnDefinition = "jsonb")
    private ParametersOptuna parametersOptuna;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_results", columnDefinition = "jsonb")
    private MetricsResults metricsResults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "baseline_distributions", columnDefinition = "jsonb")
    private Map<String, Object> baselineDistributions;

    @Column(name = "id_training_model")
    private Long idTrainingModel;

    // Relación recursiva ManyToMany: Modelos que retaron a este (cuando este estaba
    // en producción)
    @ManyToMany
    @JoinTable(name = "model_comparison", joinColumns = @JoinColumn(name = "id_production_model"), inverseJoinColumns = @JoinColumn(name = "id_candidate_model"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TrainingHistory> challengerModels = new HashSet<>();

    // Modelos que este retó (cuando este fue candidato)
    @ManyToMany(mappedBy = "challengerModels")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TrainingHistory> challengedModels = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "id_dataset")
    private DatasetInfo datasetInfo;

    // PUEDES EXPANDIRSE EN UNA TABLA CON METADATOS DE COMPARACIÓN
}
