package com.today.fridge.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Post", description = "PostController API")
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Post API")
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody PostCreateRequest request) {
        postService.createPostWithImages(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    
    // 2. 새로 추가된 특정 유저의 최근 게시글 조회 API
    // 💡 수정: @PathVariable("userId") 명시
    @GetMapping("/users/{userId}")
    @Operation(summary = "Post API")
    public ResponseEntity<List<PostSummaryResponse>> getUserPosts(@Parameter(description = "userId") @PathVariable("userId") Long userId) {
        
        List<PostSummaryResponse> response = postService.getUserPosts(userId);
        
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 새로 추가: 커뮤니티 메인 전체 게시글 조회 API
    // ==========================================
    // 💡 수정: @RequestParam(name = "...", defaultValue = "...") 명시
    @GetMapping
    @Operation(summary = "Post API")
    public ResponseEntity<Page<PostSummaryResponse>> getAllPosts(
            @Parameter(description = "page") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "size") @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getAllPosts(page, size));
    }
    
    // 💡 수정: @PathVariable("postId") 명시
    @GetMapping("/{postId}")
    @Operation(summary = "Post API")
    public ResponseEntity<PostDetailResponse> getPostDetail(@Parameter(description = "postId") @PathVariable("postId") Long postId) {
        return ResponseEntity.ok(postService.getPostDetail(postId));
    }
    
    // 💡 수정: @PathVariable("postId") 명시
    @DeleteMapping("/{postId}")
    @Operation(summary = "Post API")
    public ResponseEntity<Void> deletePost(@Parameter(description = "postId") @PathVariable("postId") Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
    
    // ==========================================
    // 💡 게시글 수정 API (PATCH)
    // ==========================================
    // 💡 수정: @PathVariable("postId") 명시
    @PatchMapping("/{postId}")
    @Operation(summary = "Post API")
    public ResponseEntity<Map<String, Object>> updatePost(
            @Parameter(description = "postId") @PathVariable("postId") Long postId, 
            @RequestBody PostUpdateRequest request) {
        
        postService.updatePost(postId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}