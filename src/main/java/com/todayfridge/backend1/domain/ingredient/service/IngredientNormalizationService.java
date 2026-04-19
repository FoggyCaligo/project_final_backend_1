package com.todayfridge.backend1.domain.ingredient.service;

import com.todayfridge.backend1.domain.ingredient.entity.IngredientMaster;
import com.todayfridge.backend1.domain.ingredient.repository.IngredientMasterRepository;
import com.todayfridge.backend1.global.external.fastapi.FastApiClient;
import com.todayfridge.backend1.global.external.fastapi.dto.CategorizeIngredientResponse;
import com.todayfridge.backend1.global.external.fastapi.dto.NormalizeIngredientResponse;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngredientNormalizationService {
    private final FastApiClient fastApiClient;
    private final IngredientMasterRepository ingredientMasterRepository;

    public IngredientNormalizationService(FastApiClient fastApiClient, IngredientMasterRepository ingredientMasterRepository) {
        this.fastApiClient = fastApiClient;
        this.ingredientMasterRepository = ingredientMasterRepository;
    }

    public Map<String, Object> normalize(String rawName) {
        NormalizeIngredientResponse normalized = fastApiClient.normalizeIngredient(rawName);
        CategorizeIngredientResponse categorized = fastApiClient.categorizeIngredient(normalized.normalizedName());
        ingredientMasterRepository.findByNormalizedName(normalized.normalizedName())
                .orElseGet(() -> ingredientMasterRepository.save(
                        new IngredientMaster(normalized.normalizedName(), categorized.category())
                ));
        return Map.of(
                "normalizedName", normalized.normalizedName(),
                "category", categorized.category(),
                "confidence", normalized.confidence(),
                "candidates", normalized.candidates()
        );
    }
}
