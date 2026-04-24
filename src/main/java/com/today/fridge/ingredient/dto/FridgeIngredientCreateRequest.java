package com.today.fridge.ingredient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FridgeIngredientCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String rawName;

    @Size(max = 100)
    private String normalizedNameSnapshot;

    private Long ingredientMasterId;

    private BigDecimal quantity;

    @Size(max = 20)
    private String unit;

    @Size(max = 30)
    private String storageType;

    private LocalDate expirationDate;
}
