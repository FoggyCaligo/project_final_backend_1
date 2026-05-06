package com.today.fridge.post.controller;

import com.today.fridge.post.service.PostFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/{userId}/follow")
@RequiredArgsConstructor
public class PostFollowController {

    private final PostFollowService postFollowService;

    // 팔로우 추가 (POST)
    @PostMapping
    public ResponseEntity<Map<String, Object>> addFollow(
            @PathVariable("userId") Long followeeId,
            @RequestParam("followerId") Long followerId) {
        
        postFollowService.addFollow(followerId, followeeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 팔로우 취소 (DELETE)
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeFollow(
            @PathVariable("userId") Long followeeId,
            @RequestParam("followerId") Long followerId) {
        
        postFollowService.removeFollow(followerId, followeeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 팔로우 여부 확인 (GET)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @PathVariable("userId") Long followeeId,
            @RequestParam("followerId") Long followerId) {
        
        boolean isFollowing = postFollowService.checkFollowStatus(followerId, followeeId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFollowing", isFollowing);
        return ResponseEntity.ok(response);
    }
}