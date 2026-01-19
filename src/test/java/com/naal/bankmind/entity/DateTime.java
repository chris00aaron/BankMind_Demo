package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "date_time")
public class DateTime {

    @Id
    @Column(name = "id_date")
    private LocalDate idDate;

    @Column(name = "day_of_the_week", nullable = false)
    private Short dayOfTheWeek;

    @Column(name = "day_of_month", nullable = false)
    private Short dayOfMonth;

    @Column(name = "month", nullable = false)
    private Short month;

    @Column(name = "is_holiday")
    private Boolean isHoliday;
}
