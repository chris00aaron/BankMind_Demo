package com.naal.bankmind.entity.Default;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.naal.bankmind.entity.Default.POJO.DetalleColumna;

import jakarta.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "dataset_info")
public class DatasetInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dataset")
    private Long idDataset;

    @Column(name = "source_data", length = 150)
    private String sourceData;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "data_amount")
    private Integer dataAmount;

    @Column(name = "data_testing")
    private Integer dataTesting;

    @Column(name = "data_training")
    private Integer dataTraining;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "columns_info", columnDefinition = "jsonb")
    private List<DetalleColumna> columnsInfo;
}
