package com.today.fridge.meal.service;

/*
 * MealServiceHelperMethods는 여러 식단 서비스에서 공통으로 사용하는 계산 및 데이터 처리 로직을 모아둔 컴포넌트입니다.
 * updateDayNutrition: 식단 기록 시 해당 날짜의 총 영양 섭취량을 계산하여 DayNutrition 테이블에 반영합니다.
 * calculateDetailedTargets: 사용자의 신체 정보(BMR, TDEE, BMI)를 기반으로 상세 영양 목표치를 산출합니다.
 * buildRemainingResponse: 목표량 대비 현재 섭취량을 비교하여 남은 영양성분 응답 객체를 생성합니다.
 */

import com.today.fridge.meal.dto.request.PhysicalMetricsRequest;
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.dto.response.RemainingNutritionResponse;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MealServiceHelperMethods {

    private final DayNutritionRepository dayNutritionRepository;

    // ============================================================================================
    // 일일 영양 섭취량 누적 업데이트
    // ============================================================================================
    public void updateDayNutrition(User user, Recipe recipe, BigDecimal servings, LocalDateTime consumedAt) {
        log.info("[MealServiceHelperMethods] updateDayNutrition (public) - userId: {}, recipeId: {}, servings: {}", user.getUserId(), recipe.getRecipeId(), servings);
        // null 또는 0인분인 경우 업데이트를 수행하지 않음
        if (servings == null || servings.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 식사 날짜(시간 제외) 기준 데이터 조회 또는 생성
        LocalDateTime dateOnly = consumedAt.toLocalDate().atStartOfDay();

        DayNutrition dayNutrition = dayNutritionRepository.findByUserUserIdAndDate(user.getUserId(), dateOnly)
                .orElseGet(() -> {
                    DayNutrition dn = new DayNutrition();
                    dn.setUser(user);
                    dn.setDate(dateOnly);
                    dn.setTotalCalories(BigDecimal.ZERO);
                    dn.setTotalCarbs(BigDecimal.ZERO);
                    dn.setTotalProtein(BigDecimal.ZERO);
                    dn.setTotalFat(BigDecimal.ZERO);
                    dn.setTotalSugar(BigDecimal.ZERO);
                    dn.setTotalSodium(BigDecimal.ZERO);
                    dn.setTotalCholesterol(BigDecimal.ZERO);
                    return dn;
                });

        // 레시피의 영양 정보 조회
        RecipeNutrition rn = recipe.getRecipeNutrition();
        if (rn != null) {
            // 레시피의 1인분 기준량 추출
            String servingsText = recipe.getServingsText();
            BigDecimal recipeServings = BigDecimal.ONE;
            if (servingsText != null && !servingsText.isEmpty()) {
                try {
                    // crawler sould only return either one of these two
                    recipeServings = new BigDecimal(servingsText.replace("인분 이상", "").replace("인분", "").trim());
                } catch (Exception e) {
                    recipeServings = BigDecimal.ONE;
                }
            }
            if (recipeServings.compareTo(BigDecimal.ZERO) == 0)
                recipeServings = BigDecimal.ONE;

            // 실제 섭취 비율 계산
            BigDecimal multiplier = servings.divide(recipeServings, 4, RoundingMode.HALF_UP);

            // 누적 영양소 업데이트 (칼로리, 탄, 단, 지, 당류, 나트륨, 콜레스테롤)
            dayNutrition.setTotalCalories(dayNutrition.getTotalCalories().add(rn.getCalories().multiply(multiplier)));
            dayNutrition.setTotalCarbs(dayNutrition.getTotalCarbs().add(rn.getCarbs().multiply(multiplier)));
            dayNutrition.setTotalProtein(dayNutrition.getTotalProtein().add(rn.getProtein().multiply(multiplier)));
            dayNutrition.setTotalFat(dayNutrition.getTotalFat().add(rn.getFat().multiply(multiplier)));

            if (rn.getSugar() != null)
                dayNutrition.setTotalSugar(dayNutrition.getTotalSugar().add(rn.getSugar().multiply(multiplier)));
            if (rn.getSodium() != null)
                dayNutrition.setTotalSodium(dayNutrition.getTotalSodium().add(rn.getSodium().multiply(multiplier)));
            if (rn.getCholesterol() != null)
                dayNutrition.setTotalCholesterol(
                        dayNutrition.getTotalCholesterol().add(rn.getCholesterol().multiply(multiplier)));
        }

        // 업데이트된 일일 영양 데이터 저장
        dayNutritionRepository.save(dayNutrition);
        log.info("일일 영양 합계 업데이트 완료 - 사용자 ID: {}, 날짜: {}", user.getUserId(), dateOnly.toLocalDate());
    }

    // ============================================================================================
    // 사용자 신체 정보를 기반으로 영양 목표 산출 (기본값 처리 포함)
    // ============================================================================================
    public NutritionTarget calculateTargetsWithDefaults(User user) {
        log.info("[MealServiceHelperMethods] calculateTargetsWithDefaults (public) - userId: {}", user.getUserId());
        double heightCm = user.getHeightCm() != null && user.getHeightCm() > 0 ? user.getHeightCm() : 175.0;
        double weightKg = user.getWeightKg() != null && user.getWeightKg() > 0 ? user.getWeightKg() : 70.0;
        int age = user.getAge() != null && user.getAge() > 0 ? user.getAge() : 30;
        String gender = user.getGender() != null ? user.getGender() : "MALE";

        return calculateDetailedTargets(heightCm, weightKg, age, gender);
    }

    // ============================================================================================
    // 사용자 맞춤형 일일 영양 목표치 상세 계산 (TDEE 기반)
    // ============================================================================================
    public NutritionTarget calculateDetailedTargets(double heightCm, double weightKg, int age, String gender) {
        log.info("[MealServiceHelperMethods] calculateDetailedTargets (public) - height: {}, weight: {}, age: {}, gender: {}", heightCm, weightKg, age, gender);
        // BMI 계산 (0으로 나누기 방지)
        double heightM = heightCm / 100.0;
        double bmi = (heightM > 0) ? weightKg / (heightM * heightM) : 0;

        // 기초 대사량 (BMR) 계산 (Mifflin-St Jeor 방정식)
        double bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        if ("MALE".equalsIgnoreCase(gender)) {
            bmr += 5;
        } else {
            bmr -= 161;
        }
        log.debug("사용자 기초대사량(BMR) 계산 완료 - 성별: {}, BMR: {}", gender, bmr);

        // 일일 총 에너지 소모량 (TDEE) 추정 (활동 계수 1.2 적용)
        double tdee = bmr * 1.2;

        // BMI에 따른 칼로리 가감 적용
        if (bmi >= 25.0) {
            tdee -= 300;
        } else if (bmi < 18.5) {
            tdee += 300;
        }

        // 매크로(다량 영양소) 목표량 계산 (탄: 50%, 단: 20%, 지: 30%)
        double carbsGrams = (tdee * 0.50) / 4.0;
        double proteinGrams = (tdee * 0.20) / 4.0;
        double fatGrams = (tdee * 0.30) / 9.0;

        // 당류, 나트륨, 콜레스테롤 목표 제한량 설정
        double sugarGrams = (tdee * 0.10) / 4.0;
        double sodiumMg = 2000.0;
        double cholesterolMg = 300.0;

        // 최종 계산된 영양 목표 객체 반환
        return new NutritionTarget(
                BigDecimal.valueOf(tdee).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(carbsGrams).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(proteinGrams).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(fatGrams).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(sugarGrams).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(sodiumMg).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(cholesterolMg).setScale(1, RoundingMode.HALF_UP));
    }

    // ============================================================================================
    // 일/주/월 단위의 남은 영양 목표 계산 응답 객체 생성
    // ============================================================================================
    public RemainingNutritionResponse buildRemainingResponse(NutritionTarget target, MealNutritionSummaryDTO intake,
            int days) {
        log.info("[MealServiceHelperMethods] buildRemainingResponse (public) - days: {}", days);
        BigDecimal daysMultiplier = new BigDecimal(days);

        // 기간(days)에 따른 총 영양 목표 계산
        BigDecimal targetCals = target.getCalories().multiply(daysMultiplier);
        BigDecimal targetCarbs = target.getCarbs().multiply(daysMultiplier);
        BigDecimal targetProtein = target.getProtein().multiply(daysMultiplier);
        BigDecimal targetFat = target.getFat().multiply(daysMultiplier);
        BigDecimal targetSugar = target.getSugar().multiply(daysMultiplier);
        BigDecimal targetSodium = target.getSodium().multiply(daysMultiplier);
        BigDecimal targetCholesterol = target.getCholesterol().multiply(daysMultiplier);

        // 남은 영양량 계산 (목표량 - 섭취량)
        BigDecimal remainingCalories = targetCals
                .subtract(intake.getTotalCalories() != null ? intake.getTotalCalories() : BigDecimal.ZERO);
        BigDecimal remainingCarbs = targetCarbs
                .subtract(intake.getTotalCarbs() != null ? intake.getTotalCarbs() : BigDecimal.ZERO);
        BigDecimal remainingProtein = targetProtein
                .subtract(intake.getTotalProtein() != null ? intake.getTotalProtein() : BigDecimal.ZERO);
        BigDecimal remainingFat = targetFat
                .subtract(intake.getTotalFat() != null ? intake.getTotalFat() : BigDecimal.ZERO);
        BigDecimal remainingSugar = targetSugar
                .subtract(intake.getTotalSugar() != null ? intake.getTotalSugar() : BigDecimal.ZERO);
        BigDecimal remainingSodium = targetSodium
                .subtract(intake.getTotalSodium() != null ? intake.getTotalSodium() : BigDecimal.ZERO);
        BigDecimal remainingCholesterol = targetCholesterol
                .subtract(intake.getTotalCholesterol() != null ? intake.getTotalCholesterol() : BigDecimal.ZERO);

        // 기간이 1일 이상일 경우 일일 평균 남은 량으로 변환
        if (days > 1) {
            remainingCalories = remainingCalories.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
            remainingCarbs = remainingCarbs.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
            remainingProtein = remainingProtein.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
            remainingFat = remainingFat.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
            remainingSugar = remainingSugar.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
            remainingSodium = remainingSodium.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
            remainingCholesterol = remainingCholesterol.divide(daysMultiplier, 1, RoundingMode.HALF_UP);
        }

        // 응답 객체 빌드 및 반환
        return RemainingNutritionResponse.builder()
                .remainingCalories(remainingCalories)
                .remainingCarbs(remainingCarbs)
                .remainingProtein(remainingProtein)
                .remainingFat(remainingFat)
                .remainingSugar(remainingSugar)
                .remainingSodium(remainingSodium)
                .remainingCholesterol(remainingCholesterol)
                .build();
    }

    // ============================================================================================
    // 사용자 신체 지표 업데이트 보조
    // ============================================================================================
    public void updateUserPhysicalMetrics(User user, PhysicalMetricsRequest request) {
        log.info("[MealServiceHelperMethods] updateUserPhysicalMetrics (public) - userId: {}", user.getUserId());
        user.setHeightCm(request.getHeightCm());
        user.setWeightKg(request.getWeightKg());
        user.setAge(request.getAge());
        user.setGender(request.getGender());
    }

    // ============================================================================================
    // 내부 영양 목표 데이터 구조체
    // ============================================================================================
    public static class NutritionTarget {
        private BigDecimal calories;
        private BigDecimal carbs;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal sugar;
        private BigDecimal sodium;
        private BigDecimal cholesterol;

        public NutritionTarget(BigDecimal calories, BigDecimal carbs, BigDecimal protein, BigDecimal fat,
                BigDecimal sugar, BigDecimal sodium, BigDecimal cholesterol) {
            this.calories = calories;
            this.carbs = carbs;
            this.protein = protein;
            this.fat = fat;
            this.sugar = sugar;
            this.sodium = sodium;
            this.cholesterol = cholesterol;
        }

        public BigDecimal getCalories() {
            return calories;
        }

        public BigDecimal getCarbs() {
            return carbs;
        }

        public BigDecimal getProtein() {
            return protein;
        }

        public BigDecimal getFat() {
            return fat;
        }

        public BigDecimal getSugar() {
            return sugar;
        }

        public BigDecimal getSodium() {
            return sodium;
        }

        public BigDecimal getCholesterol() {
            return cholesterol;
        }
    }
}
