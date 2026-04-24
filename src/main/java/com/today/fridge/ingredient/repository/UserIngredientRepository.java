package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.UserIngredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

    @EntityGraph(attributePaths = {"ingredientMaster"})
    @Query("""
            select ui from UserIngredient ui
            where ui.user.userId = :userId
            order by
                case
                    when ui.expiresAt is null then 3
                    when ui.expiresAt < :today then 0
                    when ui.expiresAt <= :soonEnd then 1
                    else 2
                end asc,
                ui.expiresAt asc,
                ui.updatedAt desc
            """)
    Page<UserIngredient> findFridgePage(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("soonEnd") LocalDate soonEnd,
            Pageable pageable);

    @EntityGraph(attributePaths = {"ingredientMaster"})
    @Query("""
            select ui from UserIngredient ui
            where ui.userIngredientId = :id and ui.user.userId = :userId
            """)
    Optional<UserIngredient> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser_UserId(Long userId);

    @Query("""
            select count(ui) from UserIngredient ui
            where ui.user.userId = :userId
              and ui.expiresAt is not null
              and ui.expiresAt < :today
            """)
    long countExpiredForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("""
            select count(ui) from UserIngredient ui
            where ui.user.userId = :userId
              and ui.expiresAt is not null
              and ui.expiresAt >= :today
              and ui.expiresAt <= :soonEnd
            """)
    long countSoonForUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("soonEnd") LocalDate soonEnd);
}
