package com.today.fridge.recipe.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.today.fridge.global.response.PageResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public PageResult<RecipeListResponse> getRecipes(Pageable pageable) {

        Page<Recipe> recipePage =
                recipeRepository.findByIsActiveTrue(pageable);

        List<RecipeListResponse> content =
                recipePage.getContent()
                        .stream()
                        .map(RecipeListResponse::from)
                        .toList();

        PageResponse pageInfo = new PageResponse(
                recipePage.getTotalElements(),
                recipePage.getTotalPages(),
                recipePage.getNumber(),
                recipePage.getSize()
        );

        return new PageResult<>(content, pageInfo);
    }
}
