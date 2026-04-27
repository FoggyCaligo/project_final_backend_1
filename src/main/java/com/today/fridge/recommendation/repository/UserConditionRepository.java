package com.today.fridge.recommendation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.user.entity.User;


public interface UserConditionRepository extends JpaRepository<UserCondition, Long>{

	List<UserCondition> findByUser_UserIdAndIsActiveTrue(Long userId);
	
	boolean existsByUser_UserIdAndConditionCode_ConditionId(Long userId, Long conditionId);
}
