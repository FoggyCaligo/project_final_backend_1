package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.UserIngredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface UserIngredientRepositoryCustom {

    Page<UserIngredient> searchFridgePage(
            Long userId,
            LocalDate today,
            LocalDate soonEnd,
            String freshnessStatus,
            String storageType,
            String keyword,
            String sort,
            Pageable pageable);
}
