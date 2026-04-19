package com.todayfridge.backend1.domain.ingredient.repository;

import com.todayfridge.backend1.domain.ingredient.entity.UserIngredient;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {
    List<UserIngredient> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<UserIngredient> findByIdAndUser_Id(Long id, Long userId);
    long countByUser_Id(Long userId);
    long countByUser_IdAndExpirationDateBetween(Long userId, LocalDate start, LocalDate end);
}
