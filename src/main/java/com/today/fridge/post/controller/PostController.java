package com.today.fridge.post.controller;

import com.today.fridge.post.dto.PostCreateRequest;
import com.today.fridge.post.dto.PostDetailResponse;
import com.today.fridge.post.service.PostService;
import com.today.fridge.post.dto.PostSummaryResponse;
import com.today.fridge.post.dto.PostUpdateRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

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

    // ==========================================
    // 새로 추가: 커뮤니티 메인 전체 게시글 조회 API
    // ==========================================
    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getAllPosts(page, size));
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPostDetail(postId));
    }
    
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
    
    // ==========================================
    // 💡 게시글 수정 API (PATCH)
    // ==========================================
    @PatchMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> updatePost(
            @PathVariable Long postId, 
            @RequestBody PostUpdateRequest request) {
        
        postService.updatePost(postId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}