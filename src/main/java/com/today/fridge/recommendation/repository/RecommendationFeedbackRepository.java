package com.today.fridge.recommendation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recommendation.entity.RecommendationFeedback;
import com.today.fridge.user.entity.User;


public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

	List<RecommendationFeedback> findByUser_UserId(Long userId);
}
