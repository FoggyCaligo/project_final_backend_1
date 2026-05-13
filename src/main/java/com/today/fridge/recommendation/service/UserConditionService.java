package com.today.fridge.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.recommendation.dto.request.UserConditionSaveRequest;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.ConditionSourceType;
import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserConditionService {

    private final UserRepository userRepository;
    private final ConditionCodeRepository conditionCodeRepository;
    private final UserConditionRepository userConditionRepository;

    public void saveUserConditions(Long userId, UserConditionSaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        upsert(user, "ALLERGY_MILK", request.getMilkAllergy());
        upsert(user, "ALLERGY_EGG", request.getEggAllergy());
        upsert(user, "DIET_LOW_CALORIE", request.getDiet());
        upsert(user, "LOW_SODIUM", request.getLowSodium());
    }

    private void upsert(User user, String conditionCodeValue, Boolean checked) {
        ConditionCode conditionCode = conditionCodeRepository.findByConditionCode(conditionCodeValue)
                .orElseThrow(() -> new IllegalArgumentException("조건 코드 없음: " + conditionCodeValue));

        UserCondition userCondition = userConditionRepository
                .findByUser_UserIdAndConditionCode(user.getUserId(), conditionCode)
                .orElseGet(() -> UserCondition.create(
                        user,
                        conditionCode,
                        ConditionSourceType.PROFILE
                ));

        if (Boolean.TRUE.equals(checked)) {
            userCondition.activate();
        } else {
            userCondition.deactivate();
        }

        userConditionRepository.save(userCondition);
    }
}