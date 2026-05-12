package com.today.fridge.recipe.service;

/*
* RecipeService는 현재 2개의 Method를 제공하고 있습니다.
 * getRecipes: 페이지네이션을 적용한 레시피 목록을 반환합니다.
 * getRecipe: 특정 레시피를 반환합니다.
 * 
 * Recipe/DTO/response/RecipeResponse를 사용하여 1개의 레시피 또는 모든 레세피를 제공합니다.
 * 
 * RecipeStepDTO 및 RecipeIngredientDTO는 레시피의 재료 및 단계 정보를 제공하기 위한 DTO입니다.
 * 이것이 없으면 N+1이 발생할 가능성이 있다 판단하여 작성하였습니다.
 * 이것들은 밑에 존재하는 getRecipeAllSteps와 getRecipeAllIngredients에서 사용됩니다.
*/
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

// Response DTO
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.global.response.PageResult;

//Intermediate DTO -> Step 및 Ingredient 조회
import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.intermediate.RecipeStepDTO;

// 각 table의 Entity
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.entity.RecipeStep;
import com.today.fridge.ingredient.entity.UserIngredient;

// 각 table의 Repository
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;

import jakarta.transaction.Transactional;

import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.meal.service.MealService;

// Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Global Exception
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeNutritionRepository recipeNutritionRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final MealService mealService;
    private final RecipeUnitAdapter unitAdapter;

    // ============================================================================================
    // 레시피 1개 조회
    // 비회원 전용
    // ============================================================================================
    public RecipeResponse getRecipe(Long recipeId) {
        log.info("[RecipeService] getRecipe START - recipeId: {}", recipeId);

        // 레시피 정보 조회
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
                });

        // 레시피 영양정보 조회
        RecipeNutrition nutrition = recipeNutritionRepository.findByRecipe_RecipeId(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피 영양정보를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NUTRITION_NOT_FOUND,
                            java.util.Map.of("recipeId", recipeId));
                });

        // 레시피 단계 조회
        List<RecipeStepDTO> recipeSteps = getRecipeAllSteps(recipeId);

        // 레시피 재료 조회
        List<RecipeIngredientDTO> recipeIngredients = getRecipeAllIngredients(recipeId);

        RecipeResponse response = RecipeResponse.of(recipe, nutrition, recipeSteps, recipeIngredients);
        log.info("[RecipeService] getRecipe END");
        return response;
    }

    // ============================================================================================
    // 레시피 1개 조회
    // 회원 전용
    // ============================================================================================
    public RecipeResponse getRecipe(Long recipeId, Long userId) {
        log.info("[RecipeService] getRecipe START - recipeId: {}, userId: {}", recipeId, userId);
        // 비회원 처리
        if (userId == null) {
            RecipeResponse response = getRecipe(recipeId);
            log.info("[RecipeService] getRecipe END");
            return response;
        }

        // 레시피 정보 조회
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
                });

        // 레시피 영양정보 조회
        RecipeNutrition nutrition = recipeNutritionRepository.findByRecipe_RecipeId(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피 영양정보를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NUTRITION_NOT_FOUND,
                            java.util.Map.of("recipeId", recipeId));
                });

        // 레시피 단계 조회
        List<RecipeStepDTO> recipeSteps = getRecipeAllSteps(recipeId);

        // 레시피 재료 조회
        List<RecipeIngredientDTO> recipeIngredients = getRecipeAllIngredients(recipeId);

        // 유저가 가지고 있는 재료와 비교하여 레시피 재료에 표시
        List<String> recipeIngredientNames = recipeIngredients.stream()
                .map(RecipeIngredientDTO::getIngredientName)
                .toList();

        List<UserIngredient> ownedIngredients = userIngredientRepository.findByUserIdAndIngredientNameIn(userId,
                recipeIngredientNames);

        // 레시피 재료 DTO에 소유 및 충분 여부 설정
        recipeIngredients.forEach(dto -> {
            String ingredientName = dto.getIngredientName();

            // 유저가 가진 해당 재료의 총 수량 합산 (정규화 적용)
            BigDecimal userQuantityBase = ownedIngredients.stream()
                    .filter(ui -> isMatchingIngredient(ingredientName, ui))
                    .map(ui -> getNormalizedUserQuantity(ui, ingredientName))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 레시피 재료 양 가져오기 (이미 정규화된 값: g, ml 등)
            BigDecimal requiredQuantityBase = extractNumericAmount(dto.getAmountText());

            // 소유 여부 및 충분 여부 설정
            dto.setUserQuantity(userQuantityBase);
            dto.setRequiredQuantity(requiredQuantityBase);

            if (userQuantityBase.compareTo(BigDecimal.ZERO) <= 0) {
                dto.setOwned(false);
                dto.setSufficiency("MISSING");
            } else if (userQuantityBase.compareTo(requiredQuantityBase) >= 0) {
                dto.setOwned(true);
                dto.setSufficiency("OK");
            } else {
                dto.setOwned(true);
                dto.setSufficiency("NOT_ENOUGH");
            }
        });

        RecipeResponse response = RecipeResponse.of(recipe, nutrition, recipeSteps, recipeIngredients);
        log.info("[RecipeService] getRecipe END");
        return response;
    }

    // ============================================================================================
    // 레시피를 먹었을 경우, 냉장고에서 사용된 재료를 차감하거나 삭제하는 기능
    // ============================================================================================
    @Transactional
    public void ateRecipe(Long recipeId, Long userId) {
        log.info("[RecipeService] ateRecipe START - recipeId: {}, userId: {}", recipeId, userId);
        if (userId == null) {
            log.warn("ateRecipe 호출 시 userId가 null입니다. 작업을 중단합니다. recipeId: {}", recipeId);
            log.info("[RecipeService] ateRecipe END");
            return;
        }

        // 1. 레시피에 필요한 모든 재료 정보 조회
        List<RecipeIngredientDTO> recipeIngredients = getRecipeAllIngredients(recipeId);

        // 2. 레시피 재료 이름 리스트 생성
        List<String> recipeIngredientNames = recipeIngredients.stream()
                .map(RecipeIngredientDTO::getIngredientName)
                .toList();

        // 3. 유저가 소유한 재료 중 레시피에 필요한 것들만 한 번에 조회
        List<UserIngredient> ownedIngredients = userIngredientRepository.findByUserIdAndIngredientNameIn(userId,
                recipeIngredientNames);

        // 4. 각 레시피 재료에 대해 차감 로직 수행
        recipeIngredients.forEach(ri -> {
            String ingredientName = ri.getIngredientName();
            BigDecimal remainingRequiredBase = extractNumericAmount(ri.getAmountText());

            // 요구 수량이 0 이하라면 차감할 필요 없음
            if (remainingRequiredBase.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }

            // 5. 현재 레시피 재료와 매칭되는 유저의 냉장고 재료 필터링 및 정렬
            // 정렬 기준: 유통기한 임박순(ASC), 유통기한이 같다면 수량이 적은 순(ASC)
            List<UserIngredient> targetIngredients = ownedIngredients.stream()
                    .filter(ui -> isMatchingIngredient(ingredientName, ui))
                    .sorted(Comparator
                            .comparing(UserIngredient::getExpiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(UserIngredient::getQuantity))
                    .toList();

            // 6. 매칭된 재료들을 순차적으로 차감
            for (UserIngredient ui : targetIngredients) {
                if (remainingRequiredBase.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // 유저 수량을 Base Unit(g, ml)으로 변환
                BigDecimal currentQuantityBase = getNormalizedUserQuantity(ui, ingredientName);
                BigDecimal consumedAmountBase = remainingRequiredBase.min(currentQuantityBase);

                // 차감 후 남은 양 (Base Unit)
                BigDecimal newQuantityBase = currentQuantityBase.subtract(consumedAmountBase);
                remainingRequiredBase = remainingRequiredBase.subtract(consumedAmountBase);

                // 원래 단위로 De-normalize 하여 수량 결정
                BigDecimal denormalizedQuantity = denormalizeQuantity(newQuantityBase, ui.getUnit(), ingredientName);

                // 수량이 1보다 작으면(0 < q < 1) 소수점 이하 버림 처리하여 삭제 대상으로 판단
                // 또는 수량이 0 이하라면 삭제
                if (denormalizedQuantity.compareTo(BigDecimal.ZERO) <= 0 || 
                    (denormalizedQuantity.compareTo(BigDecimal.ZERO) > 0 && denormalizedQuantity.compareTo(BigDecimal.ONE) < 0)) {
                    log.info("[RecipeService] ateRecipe - 수량이 부족하여 재료 삭제: {}, 남은수량: {}", ingredientName, denormalizedQuantity);
                    userIngredientRepository.delete(ui);
                } else {
                    // 수량이 1 이상 남아있다면 업데이트
                    ui.setQuantity(denormalizedQuantity);
                }
            }
        });

        // 7. 식단 기록 추가
        // 레시피 조리 완료 시, 자동으로 식단에 기록되도록 함 (기본 1인분)
        mealService.recordMeal(userId, recipeId, BigDecimal.ONE, java.time.LocalDateTime.now());
        log.info("[RecipeService] ateRecipe END");
    }

    // ============================================================================================
    // 유저 재료 수량을 Base Unit(g, ml)으로 정규화하는 헬퍼 함수
    // ============================================================================================
    private BigDecimal getNormalizedUserQuantity(UserIngredient ui, String ingredientName) {
        log.info("[RecipeService] getNormalizedUserQuantity START - userIngredientId: {}", ui.getUserIngredientId());
        BigDecimal quantity = ui.getQuantity() != null ? ui.getQuantity() : BigDecimal.ZERO;
        String unit = ui.getUnit();

        if (unit == null) {
            log.info("[RecipeService] getNormalizedUserQuantity END");
            return quantity;
        }

        if (unit.equalsIgnoreCase("kg") || unit.equalsIgnoreCase("L")) {
            BigDecimal res = quantity.multiply(new BigDecimal("1000"));
            log.info("[RecipeService] getNormalizedUserQuantity END");
            return res;
        }
        // "컵"이나 "모"가 유저 단위로 명시적으로 저장되어 있는 경우도 처리
        if (unit.contains("컵")) {
            BigDecimal res = quantity.multiply(new BigDecimal("100"));
            log.info("[RecipeService] getNormalizedUserQuantity END");
            return res;
        }
        if (unit.contains("모")) {
            BigDecimal res = quantity.multiply(new BigDecimal("300"));
            log.info("[RecipeService] getNormalizedUserQuantity END");
            return res;
        }
        if (unit.contains("개")) {
            BigDecimal res = unitAdapter.convertQuantityToGrams(ingredientName, quantity);
            log.info("[RecipeService] getNormalizedUserQuantity END");
            return res;
        }

        log.info("[RecipeService] getNormalizedUserQuantity END");
        return quantity;
    }

    // ============================================================================================
    // Base Unit(g, ml) 수량을 유저의 원래 단위로 역변환하는 헬퍼 함수
    // ============================================================================================
    private BigDecimal denormalizeQuantity(BigDecimal baseQuantity, String originalUnit, String ingredientName) {
        log.info("[RecipeService] denormalizeQuantity START - baseQuantity: {}, originalUnit: {}", baseQuantity,
                originalUnit);
        if (originalUnit == null) {
            log.info("[RecipeService] denormalizeQuantity END");
            return baseQuantity;
        }

        if (originalUnit.equalsIgnoreCase("kg") || originalUnit.equalsIgnoreCase("L")) {
            BigDecimal res = baseQuantity.divide(new BigDecimal("1000"), 3, java.math.RoundingMode.HALF_UP);
            log.info("[RecipeService] denormalizeQuantity END");
            return res;
        }
        if (originalUnit.contains("컵")) {
            BigDecimal res = baseQuantity.divide(new BigDecimal("100"), 3, java.math.RoundingMode.HALF_UP);
            log.info("[RecipeService] denormalizeQuantity END");
            return res;
        }
        if (originalUnit.contains("모")) {
            BigDecimal res = baseQuantity.divide(new BigDecimal("300"), 3, java.math.RoundingMode.HALF_UP);
            log.info("[RecipeService] denormalizeQuantity END");
            return res;
        }
        if (originalUnit.contains("개")) {
            BigDecimal res = unitAdapter.convertGramsToQuantity(ingredientName, baseQuantity);
            log.info("[RecipeService] denormalizeQuantity END");
            return res;
        }

        log.info("[RecipeService] denormalizeQuantity END");
        return baseQuantity;
    }

    // 전체 레시피 조회(페이징 처리됨)
    public PageResult<RecipeListResponse> getRecipes(
            String cookingType,
            String sort,
            Pageable pageable
    ) {
        log.info("[RecipeService] getRecipes (public) - cookingType: {}, sort: {}, pageable: {}", cookingType, sort, pageable);
        Sort sortSpec = Sort.unsorted();

        if (sort != null) {
            switch (sort) {
                case "time_asc":
                    sortSpec = Sort.by(Sort.Direction.ASC, "cookTimeText");
                    break;
                case "name":
                    sortSpec = Sort.by(Sort.Direction.ASC, "title");
                    break;
                default:
                    sortSpec = Sort.unsorted();
            }
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
    	        sortSpec
    	);
        Page<Recipe> recipePage;

        if ("difficulty_asc".equals(sort)) {

            Pageable unsortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );

            recipePage =
                    recipeRepository.findActiveOrderByDifficultyAsc(
                            unsortedPageable
                    );

        } else if (cookingType == null || "ALL".equalsIgnoreCase(cookingType)) {

            recipePage =
                    recipeRepository.findByIsActiveTrue(sortedPageable);

        } else {

            recipePage =
                    recipeRepository.findActiveRecipesByCookingType(
                    cookingType,
                            sortedPageable
                    );
        }

        log.info("[RECIPE_SORT] sort={}, pageableSort={}", sort, sortedPageable.getSort());
        List<RecipeListResponse> content = recipePage.getContent()
                .stream()
                .map(RecipeListResponse::from)
                .toList();

        PageResponse pageInfo = new PageResponse(
                recipePage.getTotalElements(),
                recipePage.getTotalPages(),
                recipePage.getNumber(),
                recipePage.getSize());

        return new PageResult<>(content, pageInfo);
    }

    // ============================================================================================
    // 레시피의 단계 및 재료를 조회 및 반환하는 helper Method들입니다.
    // 일반 RecipeIngredient 및 RecipeStep Entity는 Recipe recipe를 포함하고 있다보니
    // 이 Method들을 사용하는 것이 좋을 것 같았습니다.
    // ============================================================================================

    private List<RecipeStepDTO> getRecipeAllSteps(Long recipeId) {
        log.info("[RecipeService] getRecipeAllSteps START - recipeId: {}", recipeId);
        List<RecipeStep> recipeSteps = recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(recipeId);
        if (recipeSteps.isEmpty()) {
            log.error("레시피 단계 정보가 없습니다. recipeId: {}", recipeId);
            log.error("RecipeService.getRecipeAllSteps에서 에러가 발생하였습니다.");
            throw new ExceptionTemplate(ErrorCode.RECIPE_STEP_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
        }

        List<RecipeStepDTO> result = recipeSteps.stream()
                .map(RecipeStepDTO::of)
                .collect(Collectors.toList());
        log.info("[RecipeService] getRecipeAllSteps END");
        return result;
    }

    private List<RecipeIngredientDTO> getRecipeAllIngredients(Long recipeId) {
        log.info("[RecipeService] getRecipeAllIngredients START - recipeId: {}", recipeId);
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository
                .findByRecipe_RecipeIdOrderBySortOrderAsc(recipeId);
        if (recipeIngredients.isEmpty()) {
            log.error("레시피 재료 정보가 없습니다. recipeId: {}", recipeId);
            log.error("RecipeService.getRecipeAllIngredients에서 에러가 발생하였습니다.");
            throw new ExceptionTemplate(ErrorCode.RECIPE_INGREDIENT_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
        }

        List<RecipeIngredientDTO> result = recipeIngredients.stream()
                .map(RecipeIngredientDTO::of)
                .collect(Collectors.toList());
        log.info("[RecipeService] getRecipeAllIngredients END");
        return result;
    }

    // ============================================================================================
    // 레시피 재료의 수량 텍스트(예: "300g", "1/2개", "1.5L")에서 숫자만 추출하는 헬퍼 함수
    // ============================================================================================
    private BigDecimal extractNumericAmount(String amountText) {
        log.info("[RecipeService] extractNumericAmount START - amountText: {}", amountText);
        BigDecimal result = extractNumericAmount(amountText, null);
        log.info("[RecipeService] extractNumericAmount END");
        return result;
    }

    private BigDecimal extractNumericAmount(String amountText, String unitField) {
        log.info("[RecipeService] extractNumericAmount START - amountText: {}, unitField: {}", amountText, unitField);
        if (amountText == null || amountText.isBlank()) {
            log.info("[RecipeService] extractNumericAmount END");
            return BigDecimal.ZERO;
        }

        // 1. 정량 비교가 필요한 단위가 포함되어 있는지 확인 (텍스트 또는 별도 단위 필드)
        String combinedText = (amountText + (unitField != null ? unitField : "")).toLowerCase();
        boolean hasStandardUnit = combinedText.contains("개") ||
                combinedText.contains("g") ||
                combinedText.contains("l") ||
                combinedText.contains("ml") ||
                combinedText.contains("kg") ||
                combinedText.contains("컵") ||
                combinedText.contains("모");

        if (!hasStandardUnit) {
            log.info("[RecipeService] extractNumericAmount END");
            return BigDecimal.ZERO;
        }

        BigDecimal amount = BigDecimal.ZERO;
        boolean extracted = false;

        // 2. 분수 형태 처리 (예: "1 1/2", "3/4")
        if (amountText.contains("/")) {
            try {
                // "1 1/2" 같은 믹스드 넘버 처리
                Pattern mixedPattern = Pattern.compile("(\\d+)\\s+(\\d+)/(\\d+)");
                Matcher mixedMatcher = mixedPattern.matcher(amountText);
                if (mixedMatcher.find()) {
                    double whole = Double.parseDouble(mixedMatcher.group(1));
                    double num = Double.parseDouble(mixedMatcher.group(2));
                    double den = Double.parseDouble(mixedMatcher.group(3));
                    amount = BigDecimal.valueOf(whole + (num / den));
                    extracted = true;
                } else {
                    // 일반 분수 "1/2" 처리
                    Pattern fractionPattern = Pattern.compile("(\\d+)/(\\d+)");
                    Matcher fractionMatcher = fractionPattern.matcher(amountText);
                    if (fractionMatcher.find()) {
                        double num = Double.parseDouble(fractionMatcher.group(1));
                        double den = Double.parseDouble(fractionMatcher.group(2));
                        amount = BigDecimal.valueOf(num / den);
                        extracted = true;
                    }
                }
            } catch (Exception e) {
                // 분수 파싱 실패 시 일반 숫자 추출로 넘어감
            }
        }

        // 3. 일반 숫자 추출 (정수 또는 실수)
        if (!extracted) {
            Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)");
            Matcher matcher = pattern.matcher(amountText);
            if (matcher.find()) {
                try {
                    amount = new BigDecimal(matcher.group(1));
                    extracted = true;
                } catch (Exception e) {
                    amount = BigDecimal.ZERO;
                }
            }
        }

        // 4. 단위 변환 (Base Unit으로 정규화)
        if (extracted) {
            if (combinedText.contains("kg") || (combinedText.contains("l") && !combinedText.contains("ml"))) {
                amount = amount.multiply(new BigDecimal("1000"));
            } else if (combinedText.contains("컵")) {
                amount = amount.multiply(new BigDecimal("100"));
            } else if (combinedText.contains("모")) {
                amount = amount.multiply(new BigDecimal("300"));
            }
        }

        log.info("[RecipeService] extractNumericAmount END");
        return amount;
    }

    // ============================================================================================
    // 재료 매칭 여부 확인
    // ============================================================================================
    private boolean isMatchingIngredient(String ingredientName, UserIngredient ui) {
        log.info("[RecipeService] isMatchingIngredient START - ingredientName: {}, userIngredientId: {}",
                ingredientName, ui.getUserIngredientId());
        if (ingredientName == null) {
            log.info("[RecipeService] isMatchingIngredient END");
            return false;
        }

        boolean result = ingredientName.equals(ui.getRawName()) ||
                ingredientName.equals(ui.getNormalizedNameSnapshot()) ||
                (ui.getIngredientMaster() != null &&
                        ingredientName.equals(ui.getIngredientMaster().getNormalizedName()));
        log.info("[RecipeService] isMatchingIngredient END");
        return result;
    }
}
