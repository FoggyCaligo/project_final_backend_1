package com.today.fridge.recommendation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recommendation.entity.ConditionCode;

public interface ConditionCodeRepository extends JpaRepository<ConditionCode, Long>{

	List<ConditionCode> findByConditionCodeInAndIsActiveTrue(List<String> conditionCodes);
	
	List<ConditionCode> findByIsActiveTrue();
}
