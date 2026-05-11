package com.today.fridge.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.today.fridge.embedding.service.RecipeEmbeddingSearchService;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.llm.service.RecommendationExplanationService;
import com.today.fridge.recipe.entity.RecipeTag;
import com.today.fridge.recipe.entity.RecipeTagSourceType;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeTagRepository;
import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.ConditionWarningDto;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationRow;
import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.llm.dto.request.RecommendationExplanationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {


    private final UserConditionRepository userConditionRepository;
    private final RecipeConditionMapRepository recipeConditionMapRepository;
    private final RecommendationScoreService recommendationScoreService;
    private final RecommendationReasonService recommendationReasonService;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final AllergyFilterService allergyFilterService;
    private final RecipeEmbeddingSearchService recipeEmbeddingSearchService;
    private final HybridRankingService hybridRankingService;
    private final RecommendationExplanationService recommendationExplanationService;
    private final RecipeTagRepository recipeTagRepository;
    private final RecommendationTagScoreService recommendationTagScoreService;
    
    private RecipeRecommendationResponse createRecipeResponse(
            RecipeRecommendationRow recipe,
            List<String> requiredIngredients,
            double conditionScore,
            List<String> conditionTags,
            List<String> ownedIngredients,
            List<ConditionWarningDto> warnings,
            double semanticScore,
            double tagScore,
            boolean useLlmExplanation
    ) {
        List<String> ignoredIngredients = List.of("물", "소금", "후추");

        List<String> matchedIngredients = requiredIngredients.stream()
                .filter(ingredient -> !ignoredIngredients.contains(ingredient))
                .filter(ownedIngredients::contains)
                .toList();

        List<String> scoringRequiredIngredients = requiredIngredients.stream()
                .filter(ingredient -> !ignoredIngredients.contains(ingredient))
                .toList();

        List<String> missingIngredients = scoringRequiredIngredients.stream()
                .filter(ingredient -> !ownedIngredients.contains(ingredient))
                .toList();

        int matchedCount = matchedIngredients.size();
        int requiredCount = scoringRequiredIngredients.size();

        double ingredientScore =
                recommendationScoreService.calculateIngredientScore(matchedCount, requiredCount);

        double matchRate =
                recommendationScoreService.calculateMatchRate(matchedCount, requiredCount);

        double totalScore =
                recommendationScoreService.calculateTotalScore(ingredientScore, conditionScore);

        double hybridScore =
                hybridRankingService.calculateHybridScore(
                        totalScore,
                        semanticScore,
                        tagScore
                );

        String reason = recommendationReasonService.buildReason(
                Math.round(matchRate * 10) / 10.0,
                conditionTags,
                missingIngredients
        );

        String llmExplanation = useLlmExplanation
                ? recommendationExplanationService.generateExplanation(
                        new RecommendationExplanationContext(
                                recipe.recipeId(),
                                recipe.title(),
                                matchedIngredients,
                                missingIngredients,
                                conditionTags,
                                Math.round(matchRate * 10) / 10.0,
                                Math.round(totalScore * 10) / 10.0,
                                Math.round(semanticScore * 1000) / 1000.0,
                                Math.round(hybridScore * 10) / 10.0,
                                reason
                        )
                )
                : null;

        return RecipeRecommendationResponse.builder()
                .recipeId(recipe.recipeId())
                .title(recipe.title())
                .summary(recipe.summary())
                .cookTimeText(recipe.cookTimeText())
                .thumbnailUrl(recipe.thumbnailUrl())
                .matchRate(Math.round(matchRate * 10) / 10.0)
                .totalScore(Math.round(totalScore * 10) / 10.0)
                .semanticScore(Math.round(semanticScore * 1000) / 1000.0)
                .tagScore(Math.round(tagScore * 10) / 10.0)
                .hybridScore(Math.round(hybridScore * 10) / 10.0)
                .matchedIngredients(matchedIngredients)
                .missingIngredients(missingIngredients)
                .conditionTags(conditionTags)
                .warnings(warnings)
                .ownedIngredients(ownedIngredients)
                .substituteSuggestions(List.of())
                .reason(reason)
                .llmExplanation(llmExplanation)
                .build();
    }
    private List<ConditionWarningDto> buildWarnings(
            List<String> activeConditionCodes,
            List<RecipeConditionMap> recipeConditions
    ) {
        return recipeConditions.stream()
                .filter(rc -> activeConditionCodes.contains(
                        rc.getConditionCode().getConditionCode()
                ))
                .filter(rc -> "CAUTION".equals(rc.getFitType()))
                .map(rc -> ConditionWarningDto.builder()
                        .conditionCode(rc.getConditionCode().getConditionCode())
                        .conditionName(rc.getConditionCode().getConditionName())
                        .warningMessage(
                                rc.getConditionCode().getConditionName()
                                        + " 조건에 주의가 필요한 레시피입니다."
                        )
                        .build())
                .toList();
    }
    public PageResult<RecipeRecommendationResponse> recommend(Long userId, Pageable pageable) {
        return recommend(
                RecommendationQuery.builder()
                        .userId(userId)
                        .conditionCodes(List.of())
                        .includeIngredients(List.of())
                        .excludeIngredients(List.of())
                        .keywords(List.of())
                        .sortHint("ingredient_match")
                        .source("HOME")
                        .useUserProfile(true)
                        .useUserFridge(true)
                        .build(),
                   pageable
        );
    }
    
    public PageResult<RecipeRecommendationResponse> recommend(RecommendationQuery query, Pageable pageable) {
    	List<String> ownedIngredients =
    	        query.isUseUserFridge() && query.getUserId() != null
    	                ? userIngredientRepository.findOwnedIngredientNamesByUserId(query.getUserId())
    	                : query.getIncludeIngredients() == null
    	                        ? List.of()
    	                        : query.getIncludeIngredients();
    	log.info("[OWNED_INGREDIENTS] {}", ownedIngredients);

    	List<RecipeRecommendationRow> recipes =
    	        recipeRepository.findRecommendationRows();
        
        List<Long> recipeIds = recipes.stream()
                .map(RecipeRecommendationRow::recipeId)
                .toList();

        Map<Long, List<String>> requiredIngredientMap =
                recipeIngredientRepository.findRequiredIngredientNamesByRecipeIds(recipeIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                RecipeIngredientRepository.RecipeIngredientNameRow::getRecipeId,
                                Collectors.mapping(
                                        RecipeIngredientRepository.RecipeIngredientNameRow::getIngredientName,
                                        Collectors.toList()
                                )
                        ));

        Map<Long, List<RecipeConditionMap>> recipeConditionMap =
                recipeConditionMapRepository.findByRecipe_RecipeIdIn(recipeIds)
                        .stream()
                        .collect(Collectors.groupingBy(rc -> rc.getRecipe().getRecipeId()));

        List<UserCondition> userConditions =
                query.isUseUserProfile() && query.getUserId() != null
                        ? userConditionRepository.findByUser_UserIdAndIsActiveTrue(query.getUserId())
                        : List.of();
        List<String> activeConditionCodes = java.util.stream.Stream.concat(
                userConditions.stream()
                        .map(uc -> uc.getConditionCode().getConditionCode()),
                query.getConditionCodes() == null
                        ? java.util.stream.Stream.empty()
                        : query.getConditionCodes().stream()
        )
        .distinct()
        .toList();
        log.info("[ACTIVE_CONDITIONS] {}", activeConditionCodes);
        // 사용자 알러지 코드 추출 (condition_code reuse 중이면)
        List<String> userAllergenCodes =
                userConditions.stream()
                        .map(uc -> uc.getConditionCode().getConditionCode())
                        .filter(code -> code.startsWith("ALLERGY"))
                        .map(code -> code.replace("ALLERGY_", ""))
                        .toList();


        // hard filter
        if (!userAllergenCodes.isEmpty()) {
            recipes = recipes.stream()
                    .filter(recipe -> {
                    	List<String> recipeIngredients =
                    	        requiredIngredientMap.getOrDefault(recipe.recipeId(), List.of());

                        return !allergyFilterService.containsAllergen(
                                recipeIngredients,
                                userAllergenCodes
                        );
                    })
                    .toList();
        }
        boolean useHybridRanking =
                "CHATBOT".equalsIgnoreCase(query.getSource());

        final Map<Long, Double> semanticScoreMap;

        if (useHybridRanking) {

        	String semanticQuery = String.join(" ", query.getKeywords());

        	if (query.isUseUserFridge() && !ownedIngredients.isEmpty()) {
        	    semanticQuery = semanticQuery + " " + String.join(" ", ownedIngredients);
        	}

        	if (semanticQuery.isBlank()) {
        	    semanticQuery = String.join(" ", ownedIngredients);
        	}

            var semanticResults =
                    recipeEmbeddingSearchService.searchSimilarRecipes(
                            semanticQuery,
                            200
                    );
            semanticResults.stream().limit(5).forEach(r ->
            log.info("[SEMANTIC_TOP] recipeId={}, distance={}",
                r.getRecipeId(),
                r.getDistance()
            )
        );

            semanticScoreMap =
                    semanticResults.stream()
                            .collect(Collectors.toMap(
                                    r -> r.getRecipeId(),
                                    r -> hybridRankingService.toSemanticScore(
                                            r.getDistance()
                                    )
                            ));
        } else {
            semanticScoreMap = Map.of();
        }
        Map<Long, List<RecipeTag>> recipeTagMap =
                recipeTagRepository.findByRecipeIdIn(
                        recipes.stream()
                                .map(RecipeRecommendationRow::recipeId)
                                .toList()
                )
                .stream()
                .collect(Collectors.groupingBy(RecipeTag::getRecipeId));
        List<RecipeRecommendationResponse> responses = recipes.stream()
                .map(recipe -> {
                	List<String> requiredIngredients =
                	        requiredIngredientMap.getOrDefault(recipe.recipeId(), List.of());

                	List<RecipeConditionMap> recipeConditions =
                	        recipeConditionMap.getOrDefault(recipe.recipeId(), List.of());

                	if (useHybridRanking
                	        && query.getConditionCodes() != null
                	        && !query.getConditionCodes().isEmpty()) {

                	    boolean hasCautionForRequestedCondition = recipeConditions.stream()
                	            .anyMatch(rc ->
                	                    query.getConditionCodes().contains(
                	                            rc.getConditionCode().getConditionCode()
                	                    )
                	                    && "CAUTION".equals(rc.getFitType())
                	            );

                	    if (hasCautionForRequestedCondition) {
                	        return null;
                	    }
                	}

                	double conditionScore =
                	        recommendationScoreService.calculateConditionScoreByCodes(
                	                activeConditionCodes,
                	                recipeConditions
                	        );
                    double requestedBoost = 0.0;
                    
                    List<String> boostTargetIngredients =
                            query.getIncludeIngredients() != null && !query.getIncludeIngredients().isEmpty()
                                    ? query.getIncludeIngredients()
                                    : ownedIngredients;
                    
                    if (useHybridRanking && !boostTargetIngredients.isEmpty()) {

                        Set<String> normalizedOwned = boostTargetIngredients.stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet());

                        long matchedRequiredCount = requiredIngredients.stream()
                                .map(String::toLowerCase)
                                .filter(ing ->
                                        normalizedOwned.stream()
                                                .anyMatch(owned -> ing.contains(owned) || owned.contains(ing))
                                )
                                .count();

                        int requiredCount = requiredIngredients.size();

                        double ingredientCoverage = requiredCount == 0
                                ? 0.0
                                : (double) matchedRequiredCount / requiredCount;

                        requestedBoost = ingredientCoverage * 25.0;
                        
                 
                    }
                    List<String> conditionTags = activeConditionCodes;
                    
                    List<ConditionWarningDto> warnings = buildWarnings(
                            activeConditionCodes,
                            recipeConditions
                    );
                    double semanticScore =
                            semanticScoreMap.getOrDefault(recipe.recipeId(), 0.0);
                    List<RecipeTag> allTags =
                            recipeTagMap.getOrDefault(recipe.recipeId(), List.of());

                    List<RecipeTag> llmTags = allTags.stream()
                            .filter(tag -> tag.getSourceType() == RecipeTagSourceType.LLM)
                            .toList();

                    List<RecipeTag> tags = llmTags.isEmpty() ? allTags : llmTags;

                    double tagScore = useHybridRanking
                            ? recommendationTagScoreService.calculateTagScore(
                                    String.join(" ", query.getKeywords()),
                                    tags
                            )
                            : 0.0;
                    // 태그는 맞는데 semantic 유사도가 너무 낮으면 태그 신뢰도 낮춤
                    if (useHybridRanking && tagScore > 0 && semanticScore < 0.5) {
                        tagScore = 0.0;
                    }
                    return createRecipeResponse(
                            recipe,
                            requiredIngredients,
                            conditionScore + requestedBoost,
                            conditionTags,
                            ownedIngredients,
                            warnings,
                            semanticScore,
                            tagScore,
                            false
                    );
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> {
                    if (useHybridRanking) {
                        return Double.compare(b.getHybridScore(), a.getHybridScore());
                    }
                    return Double.compare(b.getTotalScore(), a.getTotalScore());
                })
                .toList();
        if (useHybridRanking) {
            responses.stream()
                    .limit(10)
                    .forEach(r -> log.info(
                            "[RANKING_TOP] recipeId={}, title={}, totalScore={}, semanticScore={}, tagScore={}, hybridScore={}",
                            r.getRecipeId(),
                            r.getTitle(),
                            r.getTotalScore(),
                            r.getSemanticScore(),
                            r.getTagScore(),
                            r.getHybridScore()
                    ));
        }
        List<RecipeRecommendationResponse> finalResponses = useHybridRanking
                ? attachLlmExplanationToTopN(responses, 3)
                : responses;

        List<RecipeRecommendationResponse> content;
        PageResponse pageInfo;

        if (pageable.isUnpaged()) {
            content = finalResponses;

            pageInfo = new PageResponse(
                    finalResponses.size(),
                    1,
                    0,
                    finalResponses.size()
            );
        } else {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), finalResponses.size());

            content = start >= finalResponses.size()
                    ? List.of()
                    : finalResponses.subList(start, end);

            pageInfo = new PageResponse(
                    finalResponses.size(),
                    (int) Math.ceil((double) finalResponses.size() / pageable.getPageSize()),
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
        }

        return new PageResult<>(content, pageInfo);
    }
    private List<RecipeRecommendationResponse> attachLlmExplanationToTopN(
            List<RecipeRecommendationResponse> responses,
            int limit
    ) {
        return java.util.stream.IntStream.range(0, responses.size())
                .mapToObj(i -> {
                    RecipeRecommendationResponse response = responses.get(i);

                    if (i >= limit) {
                        return response;
                    }

                    String explanation = recommendationExplanationService.generateExplanation(
                            new RecommendationExplanationContext(
                                    response.getRecipeId(),
                                    response.getTitle(),
                                    response.getMatchedIngredients(),
                                    response.getMissingIngredients(),
                                    response.getConditionTags(),
                                    response.getMatchRate(),
                                    response.getTotalScore(),
                                    response.getSemanticScore(),
                                    response.getHybridScore(),
                                    response.getReason()
                            )
                    );

                    return response.toBuilder()
                            .llmExplanation(explanation)
                            .build();
                })
                .toList();
    }
    
}