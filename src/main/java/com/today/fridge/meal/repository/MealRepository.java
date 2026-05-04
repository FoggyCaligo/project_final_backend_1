package com.today.fridge.meal.repository;

/*
 * 식단 기록 데이터베이스 처리 담당 클래스입니다.
 * 
 * 주요 기능:
 * 1. 특정 기간 동안의 영양소 섭취 요약 계산 (인분 수 기준 가중치 적용)
 *    - getNutritionSummaryByDateRange(userId, startDateTime, endDateTime)
 * 2. 특정 기간 내 식단 로그 목록 조회
 *    - findByUserIdAndConsumedAtBetween(userId, startDateTime, endDateTime)
 * 3. 특정 기간 내 총 식사 횟수 카운트
 *    - countByUserUserIdAndConsumedAtBetween(userId, startDateTime, endDateTime)
 * 4. 특정 기간 내 일일 최대 식사 횟수 조회 (데이터 대치 및 통계용)
 *    - findMaxMealsPerDayInPeriodNative(userId, startDate, endDate)
 */

// JPA Repository
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Entity
import com.today.fridge.meal.entity.Meal;
import com.today.fridge.meal.dto.response.MealLogResponse;
// DTO
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;

// Java Time
import java.time.LocalDateTime;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {

        /**
         * 특정 사용자의 지정된 기간 내 영양소 섭취 합계를 계산합니다.
         * 
         * [계산 로직 상세]
         * - 레시피의 기준 인분(r.servingsText, 예: '2인분')에서 숫자를 추출합니다.
         * - 사용자가 실제로 섭취한 양(m.servings)을 기준 인분으로 나누어 가중치를 계산합니다.
         * - 가중치를 영양 성분(rn.*)에 곱하여 총합을 SUM 합니다.
         * - 데이터가 없는 경우를 대비해 COALESCE를 사용하여 0을 반환하도록 처리했습니다.
         * 
         * @param userId        사용자 식별 ID
         * @param startDateTime 조회 시작 일시
         * @param endDateTime   조회 종료 일시 (exclusive)
         * @return 영양소 합계가 담긴 MealNutritionSummaryDTO
         */

        // 몇 인분을 섭취했는지 받은 후 recipes.serving_text를 double로 변환하여 계산
        // r.servingsText는 "2인분"과 같은 형태로 되어 있음
        // m.servings는 사용자가 입력한 섭취량
        // 예시: 사용자가 "1.5인분"을 섭취한 경우, m.servings = 1.5, r.servingsText = "2인분"
        // 계산: (1.5 / 2.0) * r.calories
        @Query("""
                            SELECT new com.today.fridge.meal.dto.response.MealNutritionSummaryDTO(
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.calories), 0),
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.carbs), 0),
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.protein), 0),
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.fat), 0),
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.sugar), 0),
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.sodium), 0),
                                COALESCE(SUM((m.servings / cast(REPLACE(r.servingsText, '인분', '') as double)) * rn.cholesterol), 0)
                            )
                            FROM Meal m
                            JOIN m.recipe r
                            JOIN r.recipeNutrition rn
                            WHERE m.user.userId = :userId
                            AND m.consumedAt >= :startDateTime
                            AND m.consumedAt < :endDateTime
                        """)
        MealNutritionSummaryDTO getNutritionSummaryByDateRange(
                        @Param("userId") Long userId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);

        /**
         * 특정 사용자의 지정된 기간 내 식단 기록 목록을 DTO 형태로 조회합니다.
         * 
         * @param userId        사용자 식별 ID
         * @param startDateTime 조회 시작 일시
         * @param endDateTime   조회 종료 일시
         * @return 식단 로그 응답 객체(MealLogResponse) 리스트
         */
        @Query("""
                        SELECT new com.today.fridge.meal.dto.response.MealLogResponse(
                            m.mealId,
                            r.recipeId,
                            r.title,
                            m.servings,
                            m.consumedAt
                        )
                        FROM Meal m
                        JOIN m.recipe r
                        WHERE m.user.userId = :userId
                        AND m.consumedAt >= :startDateTime
                        AND m.consumedAt < :endDateTime
                        """)
        List<MealLogResponse> findByUserIdAndConsumedAtBetween(
                        @Param("userId") Long userId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);

        /**
         * 특정 사용자의 지정된 기간 내 총 식사(기록) 횟수를 카운트합니다.
         * 리포트 통계 및 평균 섭취량 계산에 사용됩니다.
         */
        long countByUserUserIdAndConsumedAtBetween(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

        /**
         * 특정 기간 내 일일 최대 식사 횟수를 조회하는 네이티브 쿼리입니다.
         * 
         * [구현 상세]
         * - 내부 서브쿼리에서 날짜별로 그룹화하여 COUNT(*)를 수행합니다.
         * - 외부 쿼리에서 그 중 가장 큰 값(MAX)을 추출합니다.
         * - PostgreSQL의 CAST(... AS DATE)를 사용하여 시간 정보를 제외한 날짜 기준으로 그룹화합니다.
         * - 이 값은 리포트에서 데이터가 누락된 날을 대치(Imputation)할 기준값(M-1)을 결정하는 데 사용됩니다.
         * 
         * @param userId    사용자 식별 ID
         * @param startDate 조회 시작 일시
         * @param endDate   조회 종료 일시
         * @return 해당 기간 중 가장 많이 식사한 날의 식사 횟수
         */
        @Query(value = "SELECT COALESCE(MAX(meal_count), 0) FROM (SELECT COUNT(*) as meal_count FROM meal WHERE user_id = :userId AND consumed_at >= :startDate AND consumed_at < :endDate GROUP BY CAST(consumed_at AS DATE)) as daily_counts", nativeQuery = true)
        Integer findMaxMealsPerDayInPeriodNative(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}
