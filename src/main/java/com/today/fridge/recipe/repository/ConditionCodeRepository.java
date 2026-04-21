package com.today.fridge.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.ConditionCode;

public interface ConditionCodeRepository extends JpaRepository<ConditionCode, Long> {

}
