package com.today.fridge.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPhysicalMetricsRequest {
    private Double heightCm;
    private Double weightKg;
    private Integer age;
    private String gender; // "MALE" or "FEMALE"
}
