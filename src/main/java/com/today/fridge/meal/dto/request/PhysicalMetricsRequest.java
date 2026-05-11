package com.today.fridge.meal.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PhysicalMetricsRequest {
    private Double heightCm;
    private Double weightKg;
    private Integer age;
    private String gender; // "MALE" or "FEMALE"
}
