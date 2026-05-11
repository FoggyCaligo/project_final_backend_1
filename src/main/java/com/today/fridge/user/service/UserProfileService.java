package com.today.fridge.user.service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.user.dto.request.UserPhysicalMetricsRequest;
import com.today.fridge.user.dto.response.ProfileResponse;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    // 신체 정보 업데이트
    @Transactional
    public ProfileResponse updatePhysicalMetrics(String loginId, UserPhysicalMetricsRequest request) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        if (request.getHeightCm() != null) user.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) user.setWeightKg(request.getWeightKg());
        if (request.getAge() != null) user.setAge(request.getAge());
        if (request.getGender() != null) user.setGender(request.getGender());

        return ProfileResponse.from(user);
    }

    // 신체 정보 삭제 (null 처리)
    @Transactional
    public ProfileResponse clearPhysicalMetrics(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        user.setHeightCm(null);
        user.setWeightKg(null);
        user.setAge(null);
        user.setGender(null);

        return ProfileResponse.from(user);
    }
}
