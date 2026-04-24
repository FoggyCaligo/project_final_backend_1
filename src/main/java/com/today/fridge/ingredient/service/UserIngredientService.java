package com.today.fridge.ingredient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.response.PageInfo;
import com.today.fridge.ingredient.domain.FreshnessCalculator;
import com.today.fridge.ingredient.dto.FridgeIngredientCreateRequest;
import com.today.fridge.ingredient.dto.FridgeIngredientItemResponse;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryData;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserIngredientService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final IngredientMasterRepository ingredientMasterRepository;

    public UserIngredientService(
            UserIngredientRepository userIngredientRepository,
            UserRepository userRepository,
            IngredientMasterRepository ingredientMasterRepository) {
        this.userIngredientRepository = userIngredientRepository;
        this.userRepository = userRepository;
        this.ingredientMasterRepository = ingredientMasterRepository;
    }

    public FridgeIngredientListData list(Long userId, int page, int size) {
        LocalDate today = LocalDate.now(KST);
        LocalDate soonEnd = today.plusDays(FreshnessCalculator.SOON_DAYS_INCLUSIVE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<UserIngredient> result =
                userIngredientRepository.findFridgePage(userId, today, soonEnd, pageable);
        List<FridgeIngredientItemResponse> items = result.getContent().stream()
                .map(this::toItem)
                .toList();
        PageInfo pageInfo = new PageInfo(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
        return new FridgeIngredientListData(items, pageInfo);
    }

    public FridgeSummaryData summary(Long userId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate soonEnd = today.plusDays(FreshnessCalculator.SOON_DAYS_INCLUSIVE);
        long total = userIngredientRepository.countByUser_UserId(userId);
        long expired = userIngredientRepository.countExpiredForUser(userId, today);
        long soon = userIngredientRepository.countSoonForUser(userId, today, soonEnd);
        return new FridgeSummaryData(total, soon, expired);
    }

    @Transactional
    public FridgeIngredientItemResponse create(Long userId, FridgeIngredientCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        UserIngredient e = new UserIngredient();
        e.setUser(user);
        e.setRawName(req.getRawName());
        if (StringUtils.hasText(req.getNormalizedNameSnapshot())) {
            e.setNormalizedNameSnapshot(req.getNormalizedNameSnapshot());
        } else {
            e.setNormalizedNameSnapshot(req.getRawName());
        }
        e.setQuantity(req.getQuantity());
        e.setUnit(req.getUnit());
        e.setStorageType(StringUtils.hasText(req.getStorageType()) ? req.getStorageType() : "REFRIGERATED");
        e.setExpiresAt(req.getExpirationDate());

        if (req.getIngredientMasterId() != null) {
            IngredientMaster master = ingredientMasterRepository.findById(req.getIngredientMasterId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "MASTER_NOT_FOUND",
                            "식재료 마스터를 찾을 수 없습니다."));
            e.setIngredientMaster(master);
        }

        UserIngredient saved = userIngredientRepository.save(e);
        return toItem(saved);
    }

    @Transactional
    public FridgeIngredientItemResponse patch(Long userId, Long ingredientId, JsonNode node) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INGREDIENT_NOT_FOUND",
                        "식재료를 찾을 수 없습니다."));

        if (node.has("rawName")) {
            if (node.get("rawName").isNull() || node.get("rawName").asText().isBlank()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "rawName은 비울 수 없습니다.");
            }
            e.setRawName(node.get("rawName").asText());
        }
        if (node.has("normalizedNameSnapshot")) {
            if (node.get("normalizedNameSnapshot").isNull()) {
                e.setNormalizedNameSnapshot(null);
            } else {
                e.setNormalizedNameSnapshot(node.get("normalizedNameSnapshot").asText());
            }
        }
        if (node.has("ingredientMasterId")) {
            if (node.get("ingredientMasterId").isNull()) {
                e.setIngredientMaster(null);
            } else {
                long mid = node.get("ingredientMasterId").asLong();
                IngredientMaster master = ingredientMasterRepository.findById(mid)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.BAD_REQUEST,
                                "MASTER_NOT_FOUND",
                                "식재료 마스터를 찾을 수 없습니다."));
                e.setIngredientMaster(master);
            }
        }
        if (node.has("quantity")) {
            if (node.get("quantity").isNull()) {
                e.setQuantity(null);
            } else {
                e.setQuantity(new BigDecimal(node.get("quantity").asText()));
            }
        }
        if (node.has("unit")) {
            e.setUnit(node.get("unit").isNull() ? null : node.get("unit").asText());
        }
        if (node.has("storageType")) {
            e.setStorageType(node.get("storageType").isNull() ? null : node.get("storageType").asText());
        }
        if (node.has("expirationDate")) {
            if (node.get("expirationDate").isNull()) {
                e.setExpiresAt(null);
            } else {
                e.setExpiresAt(LocalDate.parse(node.get("expirationDate").asText()));
            }
        }

        UserIngredient saved = userIngredientRepository.save(e);
        return toItem(saved);
    }

    @Transactional
    public void delete(Long userId, Long ingredientId) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INGREDIENT_NOT_FOUND",
                        "식재료를 찾을 수 없습니다."));
        userIngredientRepository.delete(e);
    }

    private FridgeIngredientItemResponse toItem(UserIngredient ui) {
        Long masterId = ui.getIngredientMaster() != null ? ui.getIngredientMaster().getIngredientMasterId() : null;
        return new FridgeIngredientItemResponse(
                ui.getUserIngredientId(),
                masterId,
                ui.getRawName(),
                ui.getNormalizedNameSnapshot(),
                ui.getQuantity(),
                ui.getUnit(),
                ui.getStorageType(),
                ui.getExpiresAt(),
                ui.getFreshnessStatus());
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
