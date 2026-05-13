package com.today.fridge.recommendation.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.recommendation.dto.request.UserConditionSaveRequest;
import com.today.fridge.recommendation.service.UserConditionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserConditionController {

    private final UserConditionService userConditionService;

    @PutMapping("/me/conditions")
    public ResponseEntity<ApiResponse<Void>> saveConditions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UserConditionSaveRequest request
    ) {
        userConditionService.saveUserConditions(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(null, "사용자 조건 저장 성공")
        );
    }
}