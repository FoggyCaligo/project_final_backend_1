package com.today.fridge.meal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FastApiHealthReportResponse {
    private String summary;
    private List<String> advice;
    private List<String> meals;
    private List<String> videos;
}
