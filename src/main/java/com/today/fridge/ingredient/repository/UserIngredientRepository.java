package com.today.fridge.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.ingredient.entity.UserIngredient;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

}
