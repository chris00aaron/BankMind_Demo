package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDTO {
    private Integer id;
    private String name;
    private String description;
    private List<RuleDTO> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleDTO {
        private String field;
        private String op;
        private Object val;
    }
}
