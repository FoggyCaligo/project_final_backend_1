package com.today.fridge.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConditionWarningDto {

    private String conditionCode;
    private String conditionName;
    private String warningMessage;
}
