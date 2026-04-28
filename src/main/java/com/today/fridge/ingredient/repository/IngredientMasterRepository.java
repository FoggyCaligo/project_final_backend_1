package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.IngredientMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientMasterRepository extends JpaRepository<IngredientMaster, Long> {

    Optional<IngredientMaster> findByNormalizedNameIgnoreCase(String normalizedName);

    /**
     * 표준명 일치 우선, 그다음 {@code alias_text}(한글 별칭 등) 부분 일치.
     * {@code q}에는 와일드카드 문자를 넣지 말 것(서비스에서 제거).
     */
    @Query("""
            SELECT m FROM IngredientMaster m
            WHERE (m.isActive IS NULL OR m.isActive = true)
              AND (
                   LOWER(m.normalizedName) = LOWER(:q)
                OR (m.aliasText IS NOT NULL AND LOWER(m.aliasText) LIKE LOWER(CONCAT('%', :q, '%')))
              )
            ORDER BY CASE WHEN LOWER(m.normalizedName) = LOWER(:q) THEN 0 ELSE 1 END,
                     m.ingredientMasterId ASC
            """)
    List<IngredientMaster> findCandidatesByNormalizedNameOrAlias(@Param("q") String q, Pageable pageable);
}
