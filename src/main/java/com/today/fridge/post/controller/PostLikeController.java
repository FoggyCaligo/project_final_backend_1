package com.today.fridge.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.today.fridge.post.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "PostLike", description = "PostLikeController API")
@RequestMapping("/api/v1/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping
    @Operation(summary = "PostLike API")
    public ResponseEntity<Map<String, Object>> addLike(
            @Parameter(description = "postId") @PathVariable("postId") Long postId, 
            @Parameter(description = "userId") @RequestParam("userId") Long userId) {
        postLikeService.addLike(postId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "PostLike API")
    public ResponseEntity<Map<String, Object>> removeLike(
            @Parameter(description = "postId") @PathVariable("postId") Long postId, 
            @Parameter(description = "userId") @RequestParam("userId") Long userId) {
        postLikeService.removeLike(postId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "PostLike API")
    public ResponseEntity<Map<String, Object>> getStatus(
            @Parameter(description = "postId") @PathVariable("postId") Long postId, 
            @Parameter(description = "userId") @RequestParam("userId") Long userId) {
        Map<String, Object> statusAndCount = postLikeService.getLikeStatusAndCount(postId, userId);
        return ResponseEntity.ok(statusAndCount);
    }
}