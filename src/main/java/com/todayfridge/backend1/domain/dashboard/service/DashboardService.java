package com.todayfridge.backend1.domain.dashboard.service;

import com.todayfridge.backend1.domain.ingredient.repository.UserIngredientRepository;
import com.todayfridge.backend1.domain.recipe.service.RecommendationService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final UserIngredientRepository userIngredientRepository;
    private final RecommendationService recommendationService;

    public DashboardService(UserIngredientRepository userIngredientRepository, RecommendationService recommendationService) {
        this.userIngredientRepository = userIngredientRepository;
        this.recommendationService = recommendationService;
    }

    public Map<String, Object> home(Long userId) {
        return Map.of(
                "ingredientCount", userIngredientRepository.countByUser_Id(userId),
                "expiringIngredientCount", userIngredientRepository.countByUser_IdAndExpirationDateBetween(
                        userId, LocalDate.now(), LocalDate.now().plusDays(3)),
                "recommendableRecipeCount", recommendationService.countRecommendable(userId)
        );
    }
}
