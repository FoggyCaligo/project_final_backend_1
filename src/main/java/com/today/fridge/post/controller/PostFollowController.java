package com.today.fridge.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.today.fridge.post.service.PostFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "PostFollow", description = "PostFollowController API")
@RequestMapping("/api/v1/users/{userId}/follow")
@RequiredArgsConstructor
public class PostFollowController {

    private final PostFollowService postFollowService;

    // 팔로우 추가 (POST)
    @PostMapping
    @Operation(summary = "PostFollow API")
    public ResponseEntity<Map<String, Object>> addFollow(
            @Parameter(description = "followeeId") @PathVariable("userId") Long followeeId,
            @RequestParam("followerId") Long followerId) {
        
        postFollowService.addFollow(followerId, followeeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 팔로우 취소 (DELETE)
    @DeleteMapping
    @Operation(summary = "PostFollow API")
    public ResponseEntity<Map<String, Object>> removeFollow(
            @Parameter(description = "followeeId") @PathVariable("userId") Long followeeId,
            @RequestParam("followerId") Long followerId) {
        
        postFollowService.removeFollow(followerId, followeeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 팔로우 여부 확인 (GET)
    @GetMapping("/status")
    @Operation(summary = "PostFollow API")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @Parameter(description = "followeeId") @PathVariable("userId") Long followeeId,
            @RequestParam("followerId") Long followerId) {
        
        boolean isFollowing = postFollowService.checkFollowStatus(followerId, followeeId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFollowing", isFollowing);
        return ResponseEntity.ok(response);
    }
}