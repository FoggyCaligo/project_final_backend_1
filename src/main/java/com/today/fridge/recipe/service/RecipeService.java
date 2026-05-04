package com.today.fridge.recipe.service;

import java.util.Comparator;
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

    // ============================================================================================
    // 레시피 1개 조회
    // 비회원 전용
    // ============================================================================================
    public RecipeResponse getRecipe(Long recipeId) {

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

        return RecipeResponse.of(recipe, nutrition, recipeSteps, recipeIngredients);
    }

    // ============================================================================================
    // 레시피 1개 조회
    // 회원 전용
    // ============================================================================================
    public RecipeResponse getRecipe(Long recipeId, Long userId) {
        // 비회원 처리
        if (userId == null)
            return getRecipe(recipeId);

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

            // 유저가 가진 해당 재료의 총 수량 합산
            BigDecimal userQuantity = ownedIngredients.stream()
                    // 재료 이름이 같은 지 확인 - 원본 이름, 스냅샷 이름, 마스터 테이블 이름 모두 확인
                    .filter(ui -> isMatchingIngredient(ingredientName, ui))
                    // 재료 양 가져오기 - null 처리 포함
                    .map(ui -> ui.getQuantity() != null ? ui.getQuantity() : BigDecimal.ZERO)
                    // 합산
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 레시피 재료 양 가져오기
            BigDecimal requiredQuantity = extractNumericAmount(dto.getAmountText());

            // 소유 여부 및 충분 여부 설정
            dto.setUserQuantity(userQuantity);
            dto.setRequiredQuantity(requiredQuantity);

            if (userQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                dto.setOwned(false);
                dto.setSufficiency("MISSING");
            } else if (userQuantity.compareTo(requiredQuantity) >= 0) {
                dto.setOwned(true);
                dto.setSufficiency("OK");
            } else {
                dto.setOwned(true);
                dto.setSufficiency("NOT_ENOUGH");
            }
        });

        return RecipeResponse.of(recipe, nutrition, recipeSteps, recipeIngredients);
    }

    // ============================================================================================
    // 레시피를 먹었을 경우, 냉장고에서 사용된 재료를 차감하거나 삭제하는 기능
    // ============================================================================================
    @Transactional
    public void ateRecipe(Long recipeId, Long userId) {
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
            BigDecimal remainingRequired = extractNumericAmount(ri.getAmountText());

            // 요구 수량이 0 이하라면 차감할 필요 없음
            if (remainingRequired.compareTo(BigDecimal.ZERO) <= 0) {
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
                if (remainingRequired.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal currentQuantity = ui.getQuantity() != null ? ui.getQuantity() : BigDecimal.ZERO;
                BigDecimal consumedAmount = remainingRequired.min(currentQuantity);

                // 재료 수량 차감
                BigDecimal newQuantity = currentQuantity.subtract(consumedAmount);
                ui.setQuantity(newQuantity);
                remainingRequired = remainingRequired.subtract(consumedAmount);

                // 수량이 0 이하라면 냉장고에서 삭제
                if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    userIngredientRepository.delete(ui);
                }
            }

            // Note: remainingRequired가 여전히 0보다 크더라도 (재료가 부족한 경우),
            // 별도의 에러를 던지지 않고 현재 보유한 만큼만 모두 소진(0으로 설정/삭제) 처리함.
        });
    }

    // 전체 레시피 조회(페이징 처리됨)
    public PageResult<RecipeListResponse> getRecipes(
            String cookingType,
            String sort,
            Pageable pageable
    ) {
    	Sort sortSpec = Sort.unsorted();

    	if (sort != null) {
    	    switch (sort) {
    	        case "time_asc":
    	            sortSpec = Sort.by(Sort.Direction.ASC, "cookTimeText");
    	            break;
//    	        case "difficulty_asc":
//    	            sortSpec = Sort.by(Sort.Direction.ASC, "difficulty");
//    	            break;
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

        if (cookingType == null || "ALL".equalsIgnoreCase(cookingType)) {
            recipePage = recipeRepository.findByIsActiveTrue(sortedPageable);
        } else {
            recipePage = recipeRepository.findActiveRecipesByCookingType(
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
        // 레시피 단계 조회
        List<RecipeStep> recipeSteps = recipeStepRepository.findByRecipe_RecipeId(recipeId);
        if (recipeSteps.isEmpty()) {
            log.error("레시피 단계 정보가 없습니다. recipeId: {}", recipeId);
            log.error("RecipeService.getRecipeAllSteps에서 에러가 발생하였습니다.");
            throw new ExceptionTemplate(ErrorCode.RECIPE_STEP_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
        }

        return recipeSteps.stream()
                .map(RecipeStepDTO::of)
                .collect(Collectors.toList());
    }

    private List<RecipeIngredientDTO> getRecipeAllIngredients(Long recipeId) {
        // 레시피 재료 조회
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipe_RecipeId(recipeId);
        if (recipeIngredients.isEmpty()) {
            log.error("레시피 재료 정보가 없습니다. recipeId: {}", recipeId);
            log.error("RecipeService.getRecipeAllIngredients에서 에러가 발생하였습니다.");
            throw new ExceptionTemplate(ErrorCode.RECIPE_INGREDIENT_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
        }

        return recipeIngredients.stream()
                .map(RecipeIngredientDTO::of)
                .collect(Collectors.toList());
    }

    // ============================================================================================
    // 레시피 재료의 수량 텍스트(예: "300g", "1/2개", "1.5L")에서 숫자만 추출하는 헬퍼 함수
    // ============================================================================================
    private BigDecimal extractNumericAmount(String amountText) {
        if (amountText == null || amountText.isBlank()) {
            return BigDecimal.ZERO;
        }

        // 1. 특정 단위 변환 (컵 -> 100, 모 -> 300)
        if (amountText.contains("컵")) {
            return new BigDecimal("100");
        }
        if (amountText.contains("모")) {
            return new BigDecimal("300");
        }

        // 2. 정량 비교가 필요한 단위가 포함되어 있는지 확인
        boolean hasStandardUnit = amountText.contains("개") ||
                amountText.contains("g") ||
                amountText.contains("L") ||
                amountText.contains("mL") ||
                amountText.contains("kg");

        if (!hasStandardUnit) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = BigDecimal.ZERO;
        boolean extracted = false;

        // 3. 분수 형태 처리 (예: "1/2", "3/4")
        if (amountText.contains("/")) {
            try {
                String[] parts = amountText.split("/");
                if (parts.length == 2) {
                    double num = Double.parseDouble(parts[0].replaceAll("[^0-9.]", ""));
                    double den = Double.parseDouble(parts[1].replaceAll("[^0-9.]", ""));
                    if (den != 0) {
                        amount = BigDecimal.valueOf(num / den);
                        extracted = true;
                    }
                }
            } catch (Exception e) {
                // 분수 파싱 실패 시 일반 숫자 추출로 넘어감
            }
        }

        // 4. 일반 숫자 추출 (정수 또는 실수)
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

        // 5. 단위 변환 (kg -> g, L -> mL)
        if (extracted) {
            if (amountText.contains("kg") || (amountText.contains("L") && !amountText.contains("mL"))) {
                amount = amount.multiply(new BigDecimal("1000"));
            }
        }

        return amount;
    }

    // ============================================================================================
    // 재료 매칭 여부 확인
    // ============================================================================================
    private boolean isMatchingIngredient(String ingredientName, UserIngredient ui) {
        return ingredientName.equals(ui.getRawName()) ||
                ingredientName.equals(ui.getNormalizedNameSnapshot()) ||
                (ui.getIngredientMaster() != null &&
                        ingredientName.equals(ui.getIngredientMaster().getNormalizedName()));
    }
}
