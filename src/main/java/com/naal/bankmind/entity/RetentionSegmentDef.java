package com.naal.bankmind.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "retention_segment_def")
public class RetentionSegmentDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_segment")
    @JsonProperty("id")
    private Integer idSegment;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_json", nullable = false, columnDefinition = "jsonb")
    private String rulesJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
