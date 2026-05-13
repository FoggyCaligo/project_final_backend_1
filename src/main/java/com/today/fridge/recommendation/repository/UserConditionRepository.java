package com.today.fridge.recommendation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.UserCondition;
//import com.today.fridge.user.entity.User;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

	@Query("""
		    select uc
		    from UserCondition uc
		    join fetch uc.conditionCode cc
		    where uc.user.userId = :userId
		      and uc.isActive = true
		""")
		List<UserCondition> findActiveWithConditionCodeByUserId(
		        @Param("userId") Long userId
		);

    Optional<UserCondition> findByUser_UserIdAndConditionCode(
            Long userId,
            ConditionCode conditionCode
    );
}