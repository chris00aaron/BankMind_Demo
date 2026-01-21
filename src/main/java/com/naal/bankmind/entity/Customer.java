package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_customer")
    private Long idCustomer;

    @ManyToOne
    @JoinColumn(name = "id_gender")
    private Gender gender;

    @ManyToOne
    @JoinColumn(name = "id_education")
    private Education education;

    @ManyToOne
    @JoinColumn(name = "id_marriage")
    private Marriage marriage;

    @ManyToOne
    @JoinColumn(name = "id_country")
    private Country country;

    @ManyToOne
    @JoinColumn(name = "id_localization")
    private Localization localization;

    @Column(name = "id_registration_date")
    private LocalDateTime idRegistrationDate;

    @Column(name = "surname", length = 100)
    private String surname;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "age")
    private Integer age;

    @Column(name = "job", length = 150)
    private String job;
}
