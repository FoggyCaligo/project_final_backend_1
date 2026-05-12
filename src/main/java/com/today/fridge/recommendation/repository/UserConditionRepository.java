package com.today.fridge.recommendation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.UserCondition;
//import com.today.fridge.user.entity.User;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

    List<UserCondition> findByUser_UserIdAndIsActiveTrue(Long userId);

    Optional<UserCondition> findByUser_UserIdAndConditionCode(
            Long userId,
            ConditionCode conditionCode
    );
}