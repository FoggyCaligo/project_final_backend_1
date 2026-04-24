package com.today.fridge.ingredient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.response.PageInfo;
import com.today.fridge.ingredient.domain.FreshnessCalculator;
import com.today.fridge.ingredient.domain.StorageTypePolicy;
import com.today.fridge.ingredient.dto.CreateIngredientRequest;
import com.today.fridge.ingredient.dto.DeleteIngredientData;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryResponse;
import com.today.fridge.ingredient.dto.IngredientResponse;
import com.today.fridge.ingredient.dto.SoonItemResponse;
import com.today.fridge.ingredient.entity.IngredientCategory;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.IngredientCategoryRepository;
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
    private final IngredientCategoryRepository ingredientCategoryRepository;

    public UserIngredientService(
            UserIngredientRepository userIngredientRepository,
            UserRepository userRepository,
            IngredientMasterRepository ingredientMasterRepository,
            IngredientCategoryRepository ingredientCategoryRepository) {
        this.userIngredientRepository = userIngredientRepository;
        this.userRepository = userRepository;
        this.ingredientMasterRepository = ingredientMasterRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
    }

    public FridgeIngredientListData list(
            Long userId,
            int page,
            int size,
            String sort,
            String freshnessStatus,
            String storageType,
            String keyword) {
        LocalDate today = LocalDate.now(KST);
        LocalDate soonEnd = today.plusDays(FreshnessCalculator.SOON_DAYS_INCLUSIVE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<UserIngredient> result = userIngredientRepository.searchFridgePage(
                userId, today, soonEnd, freshnessStatus, storageType, keyword, sort, pageable);
        List<IngredientResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        PageInfo pageInfo = new PageInfo(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
        return new FridgeIngredientListData(items, pageInfo);
    }

    public FridgeSummaryResponse summary(Long userId) {
        long total = userIngredientRepository.countByUser_UserId(userId);
        long expired = userIngredientRepository.countByUser_UserIdAndFreshnessStatus(userId, "EXPIRED");
        long soon = userIngredientRepository.countByUser_UserIdAndFreshnessStatus(userId, "SOON");
        long fresh = total - expired - soon;
        if (fresh < 0) {
            fresh = 0;
        }
        List<UserIngredient> soonRows =
                userIngredientRepository.findTop5ByUser_UserIdAndFreshnessStatusOrderByExpiresAtAsc(
                        userId, "SOON");
        List<SoonItemResponse> soonItems = soonRows.stream()
                .map(ui -> new SoonItemResponse(
                        ui.getUserIngredientId(),
                        ui.getRawName(),
                        ui.getExpiresAt(),
                        ui.getFreshnessStatus()))
                .toList();
        return new FridgeSummaryResponse(total, fresh, soon, expired, soonItems);
    }

    @Transactional
    public IngredientResponse create(Long userId, CreateIngredientRequest req) {
        validateQuantity(req.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        String storage = StringUtils.hasText(req.getStorageType()) ? req.getStorageType() : "REFRIGERATED";
        StorageTypePolicy.validateOrThrow(storage);

        UserIngredient e = new UserIngredient();
        e.setUser(user);
        e.setRawName(req.getName().trim());
        e.setNormalizedNameSnapshot(req.getName().trim());
        e.setQuantity(req.getQuantity());
        e.setUnit(req.getUnit());
        e.setStorageType(storage);
        e.setExpiresAt(req.getExpirationDate());

        if (req.getCategoryId() != null) {
            IngredientCategory cat = ingredientCategoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "CATEGORY_NOT_FOUND",
                            "카테고리를 찾을 수 없습니다."));
            e.setCategory(cat);
        }

        tryAttachMaster(e);
        UserIngredient saved = userIngredientRepository.save(e);
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse patch(Long userId, Long ingredientId, JsonNode node) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INGREDIENT_NOT_FOUND",
                        "식재료를 찾을 수 없습니다."));

        if (node.has("name")) {
            if (node.get("name").isNull() || node.get("name").asText().isBlank()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다.");
            }
            String n = node.get("name").asText().trim();
            e.setRawName(n);
            e.setNormalizedNameSnapshot(n);
        }
        if (node.has("categoryId")) {
            if (node.get("categoryId").isNull()) {
                e.setCategory(null);
            } else {
                long cid = node.get("categoryId").asLong();
                IngredientCategory cat = ingredientCategoryRepository.findById(cid)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "CATEGORY_NOT_FOUND",
                                "카테고리를 찾을 수 없습니다."));
                e.setCategory(cat);
            }
        }
        if (node.has("quantity")) {
            if (node.get("quantity").isNull()) {
                e.setQuantity(null);
            } else {
                BigDecimal q = readQuantity(node.get("quantity"));
                validateQuantity(q);
                e.setQuantity(q);
            }
        }
        if (node.has("unit")) {
            e.setUnit(node.get("unit").isNull() ? null : node.get("unit").asText());
        }
        if (node.has("storageType")) {
            if (node.get("storageType").isNull()) {
                e.setStorageType(null);
            } else {
                String st = node.get("storageType").asText();
                StorageTypePolicy.validateOrThrow(st);
                e.setStorageType(st);
            }
        }
        if (node.has("expirationDate")) {
            if (node.get("expirationDate").isNull()) {
                e.setExpiresAt(null);
            } else {
                e.setExpiresAt(LocalDate.parse(node.get("expirationDate").asText()));
            }
        }

        tryAttachMaster(e);
        UserIngredient saved = userIngredientRepository.save(e);
        return toResponse(saved);
    }

    @Transactional
    public DeleteIngredientData delete(Long userId, Long ingredientId) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INGREDIENT_NOT_FOUND",
                        "식재료를 찾을 수 없습니다."));
        long id = e.getUserIngredientId();
        userIngredientRepository.delete(e);
        return new DeleteIngredientData(id);
    }

    private void tryAttachMaster(UserIngredient e) {
        e.setIngredientMaster(null);
        String norm = StringUtils.hasText(e.getNormalizedNameSnapshot())
                ? e.getNormalizedNameSnapshot().trim()
                : (e.getRawName() != null ? e.getRawName().trim() : "");
        if (!StringUtils.hasText(norm)) {
            return;
        }
        ingredientMasterRepository.findByNormalizedNameIgnoreCase(norm).ifPresent(m -> {
            if (e.getCategory() == null) {
                e.setIngredientMaster(m);
                return;
            }
            if (m.getCategory() != null
                    && m.getCategory().getCategoryId().equals(e.getCategory().getCategoryId())) {
                e.setIngredientMaster(m);
            }
        });
    }

    private IngredientResponse toResponse(UserIngredient ui) {
        return new IngredientResponse(
                ui.getUserIngredientId(),
                ui.getRawName(),
                ui.getNormalizedNameSnapshot(),
                resolveCategoryName(ui),
                ui.getExpiresAt(),
                ui.getQuantity(),
                ui.getUnit(),
                ui.getStorageType(),
                ui.getFreshnessStatus());
    }

    private String resolveCategoryName(UserIngredient ui) {
        if (ui.getIngredientMaster() != null && ui.getIngredientMaster().getCategory() != null) {
            return ui.getIngredientMaster().getCategory().getCategoryName();
        }
        if (ui.getCategory() != null) {
            return ui.getCategory().getCategoryName();
        }
        return null;
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다.");
        }
    }

    private static BigDecimal readQuantity(JsonNode n) {
        if (n.isNumber()) {
            return n.decimalValue();
        }
        return new BigDecimal(n.asText());
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
