package com.naal.bankmind.entity.atm;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@ToString(exclude = "atms")
@Entity
@Table(name = "location_type", schema = "public")
public class LocationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_location_type")
    private Integer idLocationType;

    @Column(name = "description", length = 50, nullable = false)
    private String description;

    @JsonManagedReference("locationType-atms")
    @OneToMany(mappedBy = "locationType", fetch = FetchType.LAZY)
    private List<Atm> atms = new ArrayList<>();
}
