package com.naal.bankmind.entity.atm;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

@AllArgsConstructor
@NoArgsConstructor
public class ProcessLogSync {

    private LocalDateTime timestamp;
    private String action;
    private String status;
    private Map<String, Object> details;
}
