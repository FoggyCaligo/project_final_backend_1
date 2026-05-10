package com.today.fridge.meal.service;

/*
 * MealServiceDaily는 일일 단위의 식단 조회 및 영양 분석을 담당하는 서비스입니다.
 * getMeals: 특정 날짜에 사용자가 기록한 식단 목록을 조회합니다.
 * getDailyIntake: 특정 날짜의 총 영양 섭취 요약 정보를 제공합니다.
 * getDailyRecommendation: 사용자의 신체 정보를 기반으로 일일 권장 섭취량과 맞춤형 피드백을 제공합니다.
 * getRemainingDailyNutrition: 오늘 남은 권장 영양 섭취량을 계산하여 반환합니다.
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.external.fastapi.MealRecommendationFastAPIService;
import com.today.fridge.global.external.fastapi.MealRecommendationRequest;
import com.today.fridge.global.external.fastapi.MealRecommendationResponse;
import com.today.fridge.meal.dto.response.AIRecommendationResponse;
import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.dto.response.MealLogResponse;
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServiceDaily {

        private final MealRepository mealRepository;
        private final UserRepository userRepository;
        private final DayNutritionRepository dayNutritionRepository;
        private final MealRecommendationFastAPIService mealRecommendationFastAPIService;
        private final MealServiceHelperMethods helperMethods;

        // 임시 인메모리 캐시: 내일 DB 테이블이 추가되기 전까지 무한 API 호출 방지용
        private final ConcurrentHashMap<String, AIRecommendationResponse> dailyRecommendationCache = new ConcurrentHashMap<>();

        // ============================================================================================
        // 특정 날짜의 식단 기록 조회
        // ============================================================================================

        // ============================================================================================
        // AI 기반 식단 추천 분석
        // ============================================================================================
        public AIRecommendationResponse getAIRecommendation(Long userId, boolean forceRefresh) {
                log.info("[MealServiceDaily] getAIRecommendation - userId: {}, forceRefresh: {}", userId, forceRefresh);

                String cacheKey = userId + "_" + LocalDate.now().toString();

                // 1. 강제 새로고침이 아니고 캐시에 결과가 있다면 즉시 반환
                if (!forceRefresh && dailyRecommendationCache.containsKey(cacheKey)) {
                        log.info("[MealServiceDaily] 캐시된 AI 추천 결과 반환 - userId: {}", userId);
                        return dailyRecommendationCache.get(cacheKey);
                }

                // 식단 기록이 하나도 없는지 확인 (최근 30일 기준)
                LocalDateTime start = LocalDateTime.now().minusDays(30);
                LocalDateTime end = LocalDateTime.now();
                long totalMeals = mealRepository.countByUserUserIdAndConsumedAtBetween(userId, start, end);

                if (totalMeals == 0) {
                        log.info("[MealServiceDaily] 식단 기록 부족으로 AI 추천 스킵 - userId: {}", userId);
                        return AIRecommendationResponse.builder()
                                        .report("아직 기록된 식단이 없습니다. 식단을 기록하시면 AI가 분석하여 맞춤형 추천을 제공해 드립니다!")
                                        .recommendations(java.util.Collections.emptyMap())
                                        .build();
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

                // 사용자 신체 정보 추출 (null 인 경우 기본값 적용: 175cm, 70kg, MALE)
                Double height = user.getHeightCm() != null && user.getHeightCm() > 0 ? user.getHeightCm() : 175.0;
                Double weight = user.getWeightKg() != null && user.getWeightKg() > 0 ? user.getWeightKg() : 70.0;
                String gender = user.getGender() != null ? user.getGender() : "MALE";
                Integer age = user.getAge() != null && user.getAge() > 0 ? user.getAge() : 30;

                MealRecommendationRequest req = new MealRecommendationRequest(
                                String.valueOf(userId),
                                height,
                                weight,
                                age,
                                gender);

                MealRecommendationResponse response = mealRecommendationFastAPIService.getMealRecommendation(req);

                AIRecommendationResponse finalResponse = AIRecommendationResponse.builder()
                                .report(response.report())
                                .recommendations(response.recommendations())
                                .build();

                // 2. 결과 캐시에 저장 (메모리) - TODO: 내일 DB 테이블(AIRecommendation)에 저장하도록 변경 필요
                dailyRecommendationCache.put(cacheKey, finalResponse);

                return finalResponse;
        }

        public List<MealLogResponse> getMeals(Long userId, LocalDate date) {
                log.info("[MealServiceDaily] getMeals (public) - userId: {}, date: {}", userId, date);
                // 지정된 날짜의 식단 목록 반환
                log.info("사용자 식단 데이터 조회 - 사용자 ID: {}, 날짜: {}", userId, date);
                return mealRepository.findByUserIdAndConsumedAtBetween(userId, date.atStartOfDay(),
                                date.plusDays(1).atStartOfDay());
        }

        // ============================================================================================
        // 일일 영양 권장량 및 사용자 맞춤 피드백 조회
        // ============================================================================================
        public DailyRecommendationResponse getDailyRecommendation(Long userId, LocalDate date) {
                log.info("[MealServiceDaily] getDailyRecommendation (public) - userId: {}, date: {}", userId, date);
                // 사용자 정보 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

                // 일일 권장 목표 계산 로직 호출 (중앙화된 기본값 처리 포함)
                MealServiceHelperMethods.NutritionTarget target = helperMethods.calculateTargetsWithDefaults(user);

                // 지정된 날짜의 실시간 누적 영양 정보 조회 (DayNutritionRepository 사용으로 일관성 유지)
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                MealNutritionSummaryDTO intake = dayNutritionRepository.getNutritionSummaryByDateRange(userId,
                                startOfDay,
                                endOfDay);

                // 현재 영양 섭취량 초기화 (Null-Safe 처리)
                BigDecimal currentCalories = intake.getTotalCalories() != null ? intake.getTotalCalories()
                                : BigDecimal.ZERO;
                BigDecimal currentCarbs = intake.getTotalCarbs() != null ? intake.getTotalCarbs() : BigDecimal.ZERO;
                BigDecimal currentProtein = intake.getTotalProtein() != null ? intake.getTotalProtein()
                                : BigDecimal.ZERO;
                BigDecimal currentFat = intake.getTotalFat() != null ? intake.getTotalFat() : BigDecimal.ZERO;
                BigDecimal currentSugar = intake.getTotalSugar() != null ? intake.getTotalSugar() : BigDecimal.ZERO;
                BigDecimal currentSodium = intake.getTotalSodium() != null ? intake.getTotalSodium() : BigDecimal.ZERO;
                BigDecimal currentCholesterol = intake.getTotalCholesterol() != null ? intake.getTotalCholesterol()
                                : BigDecimal.ZERO;

                // 권장량 대비 섭취량 기반 피드백 생성
                List<String> advice = new ArrayList<>();
                if (currentCalories.compareTo(target.getCalories()) > 0) {
                        advice.add("일일 권장 칼로리(" + target.getCalories() + " kcal)를 초과했습니다. 가벼운 운동을 권장합니다.");
                } else {
                        advice.add("오늘 남은 권장 칼로리는 "
                                        + target.getCalories().subtract(currentCalories).setScale(1,
                                                        RoundingMode.HALF_UP)
                                        + " kcal입니다.");
                }

                if (currentProtein.compareTo(target.getProtein()) < 0) {
                        advice.add("단백질이 풍부한 음식을 더 섭취해 보세요. "
                                        + target.getProtein().subtract(currentProtein).setScale(1, RoundingMode.HALF_UP)
                                        + "g이 더 필요합니다.");
                }

                if (currentCarbs.compareTo(target.getCarbs()) > 0) {
                        advice.add("탄수화물 권장량을 초과했습니다. 남은 하루 동안 탄수화물 섭취를 줄여보세요.");
                }

                if (currentSugar.compareTo(target.getSugar()) > 0) {
                        advice.add("일일 당류 권장량을 초과했습니다. 단 음료나 간식을 피해 보세요.");
                }

                if (currentSodium.compareTo(target.getSodium()) > 0) {
                        advice.add("일일 나트륨 권장량을 초과했습니다. 다음 식사에는 저나트륨 음식을 고려해 보세요.");
                }

                if (currentCholesterol.compareTo(target.getCholesterol()) > 0) {
                        advice.add("일일 콜레스테롤 권장량을 초과했습니다. 계란이나 고지방 유제품 같은 음식을 줄여보세요.");
                }

                // 응답 객체 생성 및 반환
                DailyRecommendationResponse response = DailyRecommendationResponse.builder()
                                .targetCalories(target.getCalories())
                                .targetCarbs(target.getCarbs())
                                .targetProtein(target.getProtein())
                                .targetFat(target.getFat())
                                .targetSugar(target.getSugar())
                                .targetSodium(target.getSodium())
                                .targetCholesterol(target.getCholesterol())
                                .currentCalories(currentCalories)
                                .currentCarbs(currentCarbs)
                                .currentProtein(currentProtein)
                                .currentFat(currentFat)
                                .currentSugar(currentSugar)
                                .currentSodium(currentSodium)
                                .currentCholesterol(currentCholesterol)
                                .advice(advice)
                                .build();
                log.info("일일 권장량 및 피드백 생성 완료 - 사용자 ID: {}, 조언 개수: {}", userId, advice.size());
                return response;
        }
}
