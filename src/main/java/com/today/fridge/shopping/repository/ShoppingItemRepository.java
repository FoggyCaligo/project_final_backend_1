package com.today.fridge.shopping.repository;

import com.today.fridge.shopping.entity.ShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    List<ShoppingItem> findByIngredientMaster_IngredientMasterIdAndExpiresAtAfterOrderByPriceAsc(
            Long ingredientMasterId, Instant now);

    @Modifying
    @Query("DELETE FROM ShoppingItem s WHERE s.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
