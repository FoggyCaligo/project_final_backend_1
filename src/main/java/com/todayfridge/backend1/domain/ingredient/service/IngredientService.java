package com.todayfridge.backend1.domain.ingredient.service;

import com.todayfridge.backend1.domain.ingredient.entity.UserIngredient;
import com.todayfridge.backend1.domain.ingredient.repository.UserIngredientRepository;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.domain.user.repository.UserRepository;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngredientService {
    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final IngredientNormalizationService ingredientNormalizationService;

    public IngredientService(UserIngredientRepository userIngredientRepository, UserRepository userRepository,
                             IngredientNormalizationService ingredientNormalizationService) {
        this.userIngredientRepository = userIngredientRepository;
        this.userRepository = userRepository;
        this.ingredientNormalizationService = ingredientNormalizationService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long userId) {
        return userIngredientRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream().map(this::toMap).toList();
    }

    public Map<String, Object> create(Long userId, String ingredientName, String quantity, String unit,
                                      LocalDate expirationDate, String storageType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Map<String, Object> normalized = ingredientNormalizationService.normalize(ingredientName);
        UserIngredient ingredient = new UserIngredient(
                user, ingredientName, (String) normalized.get("normalizedName"), (String) normalized.get("category"),
                quantity, unit, expirationDate, storageType
        );
        return toMap(userIngredientRepository.save(ingredient));
    }

    public Map<String, Object> update(Long userId, Long ingredientId, String ingredientName, String quantity, String unit,
                                      LocalDate expirationDate, String storageType) {
        UserIngredient ingredient = userIngredientRepository.findByIdAndUser_Id(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "식재료를 찾을 수 없습니다."));
        Map<String, Object> normalized = ingredientNormalizationService.normalize(ingredientName);
        ingredient.update(ingredientName, (String) normalized.get("normalizedName"), (String) normalized.get("category"),
                quantity, unit, expirationDate, storageType);
        return toMap(ingredient);
    }

    public void delete(Long userId, Long ingredientId) {
        UserIngredient ingredient = userIngredientRepository.findByIdAndUser_Id(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "식재료를 찾을 수 없습니다."));
        userIngredientRepository.delete(ingredient);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long userId) {
        return Map.of(
                "totalIngredients", userIngredientRepository.countByUser_Id(userId),
                "expiringSoonIngredients", userIngredientRepository.countByUser_IdAndExpirationDateBetween(
                        userId, LocalDate.now(), LocalDate.now().plusDays(3))
        );
    }

    private Map<String, Object> toMap(UserIngredient ingredient) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ingredientId", ingredient.getId());
        data.put("ingredientName", ingredient.getIngredientName());
        data.put("normalizedName", ingredient.getNormalizedName());
        data.put("category", ingredient.getCategory());
        data.put("quantity", ingredient.getQuantity());
        data.put("unit", ingredient.getUnit());
        data.put("expirationDate", ingredient.getExpirationDate());
        data.put("storageType", ingredient.getStorageType());
        return data;
    }
}
