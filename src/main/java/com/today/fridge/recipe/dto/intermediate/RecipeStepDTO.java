package com.today.fridge.recipe.dto.intermediate;

import com.today.fridge.recipe.entity.RecipeStep;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeStepDTO {
    private Integer stepNo;
    private String instructionText;
    private String stepImageUrl;

    public static RecipeStepDTO of(RecipeStep step) {
        return RecipeStepDTO.builder()
                .stepNo(step.getStepNo())
                .instructionText(step.getInstructionText())
                .stepImageUrl(step.getStepImageUrl())
                .build();
    }
}
