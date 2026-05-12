package com.today.fridge.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPhysicalMetricsRequest {

    @DecimalMin(value = "50.0", message = "키는 50cm 이상이어야 합니다.")
    @DecimalMax(value = "250.0", message = "키는 250cm 이하이어야 합니다.")
    private Double heightCm;

    @DecimalMin(value = "10.0", message = "몸무게는 10kg 이상이어야 합니다.")
    @DecimalMax(value = "500.0", message = "몸무게는 500kg 이하이어야 합니다.")
    private Double weightKg;

    @Min(value = 0, message = "나이는 0세 이상이어야 합니다.")
    @Max(value = 120, message = "나이는 120세 이하이어야 합니다.")
    private Integer age;

    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 'MALE' 또는 'FEMALE'이어야 합니다.")
    private String gender; // "MALE" or "FEMALE"
}

