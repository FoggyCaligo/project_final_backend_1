package com.today.fridge.post.controller;

import com.today.fridge.post.dto.PostCreateRequest;
import com.today.fridge.post.service.PostService;
import com.today.fridge.post.dto.PostSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody PostCreateRequest request) {
        postService.createPostWithImages(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    // 2. 새로 추가된 특정 유저의 최근 게시글 조회 API
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PostSummaryResponse>> getUserPosts(@PathVariable Long userId) {
        
        List<PostSummaryResponse> response = postService.getUserPosts(userId);
        
        return ResponseEntity.ok(response);
    }
}