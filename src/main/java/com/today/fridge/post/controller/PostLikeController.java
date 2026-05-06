package com.today.fridge.post.controller;

import com.today.fridge.post.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> addLike(
            @PathVariable("postId") Long postId, 
            @RequestParam("userId") Long userId) {
        postLikeService.addLike(postId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeLike(
            @PathVariable("postId") Long postId, 
            @RequestParam("userId") Long userId) {
        postLikeService.removeLike(postId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable("postId") Long postId, 
            @RequestParam("userId") Long userId) {
        Map<String, Object> statusAndCount = postLikeService.getLikeStatusAndCount(postId, userId);
        return ResponseEntity.ok(statusAndCount);
    }
}