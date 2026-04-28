package com.today.fridge.ingredient.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ingredient_master")
public class IngredientMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 최종 PostgreSQL 스키마(postgre_init) 기준 PK 컬럼은 ingredient_id
    @Column(name = "ingredient_id")
    private Long ingredientMasterId;

    /**
     * DDL 근거: {@code 오늘냉장고_MariaDB_CREATE_TABLE_초안.sql} — {@code canonical_name VARCHAR(100) NOT NULL UNIQUE}.
     * 애플리케이션에서는 ERD v1.1의 정규화 표준명과 동일한 문자열을 저장한다 ({@link #normalizedName}과 값 동일).
     */
    @Column(name = "canonical_name", nullable = false, unique = true, length = 100)
    private String canonicalName;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "alias_text", columnDefinition = "TEXT")
    private String aliasText;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}