package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.UserIngredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long>, UserIngredientRepositoryCustom {

    @EntityGraph(attributePaths = {"ingredientMaster", "fileAsset"})
    @Query("""
            select ui from UserIngredient ui
            where ui.userIngredientId = :id and ui.user.userId = :userId
            """)
    Optional<UserIngredient> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser_UserId(Long userId);

    @Query("""
            select count(u) from UserIngredient u
            where u.user.userId = :userId and u.expiresAt < :today
            """)
    long countByUser_UserIdAndExpiresAtBefore(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("""
            select count(u) from UserIngredient u
            where u.user.userId = :userId
              and u.expiresAt is not null
              and u.expiresAt >= :today
              and u.expiresAt <= :soonEnd
            """)
    long countByUser_UserIdAndExpiresAtSoonWindow(
            @Param("userId") Long userId, @Param("today") LocalDate today, @Param("soonEnd") LocalDate soonEnd);

    @EntityGraph(attributePaths = {"ingredientMaster"})
    @Query("""
            select u from UserIngredient u
            where u.user.userId = :userId
              and u.expiresAt is not null
              and u.expiresAt >= :today
              and u.expiresAt <= :soonEnd
            """)
    Page<UserIngredient> findSoonPageForSummary(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("soonEnd") LocalDate soonEnd,
            Pageable pageable);
    
    @Query("""
            select coalesce(im.normalizedName, ui.normalizedNameSnapshot, ui.rawName)
            from UserIngredient ui
            left join ui.ingredientMaster im
            where ui.user.userId = :userId
        """)
        List<String> findOwnedIngredientNamesByUserId(@Param("userId") Long userId);
}
