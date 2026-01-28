package com.naal.bankmind.entity.Default;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.naal.bankmind.entity.Default.POJO.AssemblyConfiguration;


@Data
@Entity
@Table(name = "production_model_default")
public class ProductionModelDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_production_model")
    private Long idProductionModel;

    @Column(name = "version", length = 80)
    private String version;

    @Column(name = "deployment_date")
    private LocalDateTime deploymentDate;

    @Column(name = "retire_date")
    private LocalDateTime retireDate;

    @Column(name = "auc_roc")
    private String aucRoc;

    @Column(name = "gini_coefficient")
    private String giniCoefficient;

    @Column(name = "ks_statistic")
    private String ksStatistic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assembly_configuration", columnDefinition = "jsonb")
    private AssemblyConfiguration assemblyConfiguration;

    @Column(name = "is_active")
    private Boolean isActive;

}
