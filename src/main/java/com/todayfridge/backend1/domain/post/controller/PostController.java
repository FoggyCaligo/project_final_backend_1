package com.todayfridge.backend1.domain.post.controller;

import com.todayfridge.backend1.domain.post.service.PostService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(postService.list());
    }

    @GetMapping("/{postId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long postId) {
        return ApiResponse.success(postService.detail(postId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> create(@CurrentUser LoginUser loginUser,
                                                   @RequestPart("title") String title,
                                                   @RequestPart("content") String content,
                                                   @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ApiResponse.success("게시글이 등록되었습니다.", postService.create(loginUser.getUserId(), title, content, images));
    }

    @PatchMapping("/{postId}")
    public ApiResponse<Map<String, Object>> update(@CurrentUser LoginUser loginUser, @PathVariable Long postId,
                                                   @RequestBody PostRequest request) {
        return ApiResponse.success("게시글이 수정되었습니다.",
                postService.update(loginUser.getUserId(), postId, request.title(), request.content()));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@CurrentUser LoginUser loginUser, @PathVariable Long postId) {
        postService.delete(loginUser.getUserId(), postId);
        return ApiResponse.success("게시글이 삭제되었습니다.", null);
    }

    @PostMapping("/{postId}/likes")
    public ApiResponse<Void> like(@CurrentUser LoginUser loginUser, @PathVariable Long postId) {
        postService.like(loginUser.getUserId(), postId);
        return ApiResponse.success("좋아요를 추가했습니다.", null);
    }

    @DeleteMapping("/{postId}/likes")
    public ApiResponse<Void> unlike(@CurrentUser LoginUser loginUser, @PathVariable Long postId) {
        postService.unlike(loginUser.getUserId(), postId);
        return ApiResponse.success("좋아요를 취소했습니다.", null);
    }

    public record PostRequest(String title, String content) {}
}
