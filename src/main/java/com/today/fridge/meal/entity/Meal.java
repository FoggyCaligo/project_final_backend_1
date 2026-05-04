package com.today.fridge.meal.entity;

/*
 * 이것은 식사를 기록하기 위한 Entity 입니다.
 * 초안: 
 *      1. 일단 레시피 목록 내 존재하는 음식만 기록
 *      2. 음식의 양은 1인분으로 기준으로 기록
 *      3. 영양정보는 레시피의 영양정보 사용
 *      4. 이것을 기준으로 사용자가 섭취한 영양정보를 계산
 *      5. 1일 보고는 BMI 맟 성별을 기준으로 작성
 *      6. 주별 및 월별은 Gemini를 사용하여 보고서를 작성할 예정
 *      7. 주간 및 월간 보고성에 빠진 끼니는 하루 평균 섭취량의 1/3로 가중치를 부여하여 계산
 * 
 * 추가기능:
 * 사진을 통한 기록은 AI 모델을 돌려서 레시피를 찾은후, 그 레시피의 영양정보를 가져오는 방식으로 진행할 예정입니다. 
 */
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal", indexes = {
        @Index(name = "idx_meal_user_date", columnList = "user_id, consumed_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_id")
    private Long mealId;

    // Foreign Key linking to users.user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Foreign Key linking to recipes.recipe_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    // 일반 기능에 필요 몇 인분 (1인분 기준)
    @Column(name = "servings", precision = 5, scale = 2, nullable = false)
    private BigDecimal servings = BigDecimal.valueOf(1.00);

    // 먹은 시간
    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt;

    // 생성된 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 추가 기능: 현재 null로 유지할 예정
    // 직접 입력한 음식 이름
    @Column(name = "custom_food_name", length = 100)
    private String customFoodName;

    // 이미지 기록 시 음식 이미지
    @Column(name = "source_image_url", length = 500)
    private String sourceImageUrl;

    @PrePersist
    protected void onCreate() {
        if (this.consumedAt == null) {
            this.consumedAt = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}