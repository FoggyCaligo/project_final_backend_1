package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.UserIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long>, UserIngredientRepositoryCustom {

    @EntityGraph(attributePaths = {"ingredientMaster", "ingredientMaster.category", "category"})
    @Query("""
            select ui from UserIngredient ui
            where ui.userIngredientId = :id and ui.user.userId = :userId
            """)
    Optional<UserIngredient> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser_UserId(Long userId);

    long countByUser_UserIdAndFreshnessStatus(Long userId, String freshnessStatus);

    @EntityGraph(attributePaths = {"ingredientMaster", "ingredientMaster.category", "category"})
    List<UserIngredient> findTop5ByUser_UserIdAndFreshnessStatusOrderByExpiresAtAsc(
            Long userId, String freshnessStatus);
}
