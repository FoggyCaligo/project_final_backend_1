package com.today.fridge.llm.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubstitutionLlmResponse {

    private List<SubstitutionLlmResult> results;
}