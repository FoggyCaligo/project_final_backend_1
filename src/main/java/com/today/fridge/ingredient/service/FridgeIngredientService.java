package com.today.fridge.ingredient.service;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.ingredient.domain.FreshnessCalculator;
import com.today.fridge.ingredient.domain.StorageTypePolicy;
import com.today.fridge.ingredient.dto.CategoryResponse;
import com.today.fridge.ingredient.dto.CreateIngredientRequest;
import com.today.fridge.ingredient.dto.DeleteIngredientData;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryResponse;
import com.today.fridge.ingredient.dto.IngredientResponse;
import com.today.fridge.ingredient.dto.SoonItemResponse;
import com.today.fridge.ingredient.entity.IngredientCategory;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.IngredientCategoryRepository;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.ingredient.type.FreshnessStatus;
import com.today.fridge.ingredient.type.StorageType;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FridgeIngredientService {

    private static final int NAME_MAX_LEN = 100;
    private static final int UNIT_MAX_LEN = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final IngredientCategoryRepository ingredientCategoryRepository;

    public FridgeIngredientService(
            UserIngredientRepository userIngredientRepository,
            UserRepository userRepository,
            IngredientMasterRepository ingredientMasterRepository,
            IngredientCategoryRepository ingredientCategoryRepository) {
        this.userIngredientRepository = userIngredientRepository;
        this.userRepository = userRepository;
        this.ingredientMasterRepository = ingredientMasterRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
    }

    public List<CategoryResponse> listCategories() {
        return ingredientCategoryRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(c -> new CategoryResponse(c.getCategoryId(), c.getCategoryCode(), c.getCategoryName(), c.getSortOrder()))
                .toList();
    }

    public FridgeIngredientListData list(
            Long userId,
            int page,
            int size,
            String sort,
            String freshnessStatus,
            String storageType,
            String keyword) {
        FreshnessStatus freshness = parseFreshnessFilter(freshnessStatus);
        if (StringUtils.hasText(storageType)) {
            StorageTypePolicy.validateOrThrow(storageType);
        }
        LocalDate today = LocalDate.now(KST);
        LocalDate soonEnd = today.plusDays(FreshnessCalculator.SOON_DAYS_INCLUSIVE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<UserIngredient> result = userIngredientRepository.searchFridgePage(
                userId, today, soonEnd, freshness, storageType, keyword, sort, pageable);
        List<IngredientResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        PageResponse pageInfo = new PageResponse(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
        return new FridgeIngredientListData(items, pageInfo);
    }

    public FridgeSummaryResponse summary(Long userId) {
        long total = userIngredientRepository.countByUser_UserId(userId);
        long expired = userIngredientRepository.countByUser_UserIdAndFreshnessStatus(userId, FreshnessStatus.EXPIRED);
        long soon = userIngredientRepository.countByUser_UserIdAndFreshnessStatus(userId, FreshnessStatus.SOON);
        long fresh = total - expired - soon;
        if (fresh < 0) {
            fresh = 0;
        }
        List<UserIngredient> soonRows =
                userIngredientRepository.findTop5ByUser_UserIdAndFreshnessStatusOrderByExpiresAtAsc(
                        userId, FreshnessStatus.SOON);
        List<SoonItemResponse> soonItems = soonRows.stream()
                .map(ui -> new SoonItemResponse(
                        ui.getUserIngredientId(),
                        ui.getRawName(),
                        ui.getExpiresAt(),
                        ui.getFreshnessStatus() != null ? ui.getFreshnessStatus().name() : FreshnessStatus.UNKNOWN.name()))
                .toList();
        return new FridgeSummaryResponse(total, fresh, soon, expired, soonItems);
    }

    @Transactional
    public IngredientResponse create(Long userId, CreateIngredientRequest req) {
        validateQuantity(req.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String storage = StringUtils.hasText(req.getStorageType()) ? req.getStorageType() : StorageType.REFRIGERATED.name();
        StorageTypePolicy.validateOrThrow(storage);

        UserIngredient e = new UserIngredient();
        e.setUser(user);
        e.setRawName(req.getName().trim());
        e.setNormalizedNameSnapshot(req.getName().trim());
        e.setQuantity(req.getQuantity());
        validateUnitLength(req.getUnit());
        e.setUnit(req.getUnit());
        e.setStorageType(storage);
        e.setExpiresAt(req.getExpirationDate());
        e.setCategoryId(validateCategoryId(req.getCategoryId()));

        tryAttachMaster(e);
        UserIngredient saved = userIngredientRepository.save(e);
        return toResponse(saved);
    }

    /**
     * §4.4 — {@link com.today.fridge.ingredient.dto.UpdateIngredientRequest}와 동일 키.
     * {@code Map}으로 받아 전달된 키만 반영한다.
     */
    @Transactional
    public IngredientResponse patch(Long userId, Long ingredientId, Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        if (body.containsKey("name")) {
            Object v = body.get("name");
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            String n = v.toString().trim();
            validateNameLength(n);
            e.setRawName(n);
            e.setNormalizedNameSnapshot(n);
        }
        if (body.containsKey("quantity")) {
            Object v = body.get("quantity");
            if (v == null) {
                e.setQuantity(null);
            } else {
                BigDecimal q = toBigDecimal(v);
                validateQuantity(q);
                e.setQuantity(q);
            }
        }
        if (body.containsKey("unit")) {
            Object v = body.get("unit");
            if (v == null) {
                e.setUnit(null);
            } else {
                String u = v.toString();
                if (u.length() > UNIT_MAX_LEN) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                e.setUnit(u);
            }
        }
        if (body.containsKey("storageType")) {
            Object v = body.get("storageType");
            if (v == null) {
                e.setStorageType(null);
            } else {
                String st = v.toString();
                StorageTypePolicy.validateOrThrow(st);
                e.setStorageType(st);
            }
        }
        if (body.containsKey("expirationDate")) {
            Object v = body.get("expirationDate");
            if (v == null) {
                e.setExpiresAt(null);
            } else {
                e.setExpiresAt(parseLocalDate(v));
            }
        }
        if (body.containsKey("categoryId")) {
            Object v = body.get("categoryId");
            if (v == null) {
                e.setCategoryId(null);
            } else {
                Long catId = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString());
                e.setCategoryId(validateCategoryId(catId));
            }
        }

        tryAttachMaster(e);
        UserIngredient saved = userIngredientRepository.save(e);
        return toResponse(saved);
    }

    @Transactional
    public DeleteIngredientData delete(Long userId, Long ingredientId) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        long id = e.getUserIngredientId();
        userIngredientRepository.delete(e);
        return new DeleteIngredientData(id);
    }

    private static FreshnessStatus parseFreshnessFilter(String freshnessStatus) {
        if (!StringUtils.hasText(freshnessStatus)) {
            return null;
        }
        try {
            return FreshnessStatus.valueOf(freshnessStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void tryAttachMaster(UserIngredient e) {
        e.setIngredientMaster(null);
        String norm = StringUtils.hasText(e.getNormalizedNameSnapshot())
                ? e.getNormalizedNameSnapshot().trim()
                : (e.getRawName() != null ? e.getRawName().trim() : "");
        if (!StringUtils.hasText(norm)) {
            return;
        }
        ingredientMasterRepository.findByNormalizedNameIgnoreCase(norm).ifPresent(e::setIngredientMaster);
    }

    private IngredientResponse toResponse(UserIngredient ui) {
        return new IngredientResponse(
                ui.getUserIngredientId(),
                ui.getRawName(),
                ui.getNormalizedNameSnapshot(),
                ui.getCategoryId(),
                resolveCategoryName(ui),
                ui.getExpiresAt(),
                ui.getQuantity(),
                ui.getUnit(),
                ui.getStorageType(),
                ui.getFreshnessStatus() != null ? ui.getFreshnessStatus().name() : FreshnessStatus.UNKNOWN.name());
    }

    private String resolveCategoryName(UserIngredient ui) {
        if (ui.getCategoryId() == null) {
            return null;
        }
        return ingredientCategoryRepository.findById(ui.getCategoryId())
                .map(IngredientCategory::getCategoryName)
                .orElse(null);
    }

    private Long validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (!ingredientCategoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryId;
    }

    private static void validateNameLength(String name) {
        if (name.length() > NAME_MAX_LEN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validateUnitLength(String unit) {
        if (unit != null && unit.length() > UNIT_MAX_LEN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(v.toString());
    }

    private static LocalDate parseLocalDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        return LocalDate.parse(v.toString());
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
