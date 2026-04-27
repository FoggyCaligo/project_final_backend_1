package com.today.fridge.recipe.dto.intermediate;

/*
 * Recipe Step의 경우, Recipe recipe가 존재하다 보니 N+1 발생할 수 있는 가능성이 존재함.
 * 이를 방지하기 위해 RecipeStepDTO를 사용함. 
 * 이 DTO는 Recipe/Service의 getRecipeAllSteps에서 사용됩니다.
 * 
 * of method를 사용하여 immutable한 정보를 받아와 DTO를 생성합니다.
 * 
 */
import com.today.fridge.recipe.entity.RecipeStep;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeStepDTO {
    private Integer stepNo;
    private String instructionText;
    private String stepImageUrl;

    public static RecipeStepDTO of(RecipeStep step) {
        if (step == null)
            return null;
        return RecipeStepDTO.builder()
                .stepNo(step.getStepNo())
                .instructionText(step.getInstructionText())
                .stepImageUrl(step.getStepImageUrl())
                .build();
    }
}
