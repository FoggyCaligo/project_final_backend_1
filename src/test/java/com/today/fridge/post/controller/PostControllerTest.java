package com.today.fridge.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.post.dto.PostCreateRequest;
import com.today.fridge.post.dto.PostUpdateRequest;
import com.today.fridge.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// 💡 @MockBean 대신 Spring Boot 3.4+ 에서 권장하는 @MockitoBean 임포트
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@SuppressWarnings("null") // 💡 Null type safety 경고 숨김 처리
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean // 💡 @MockBean을 @MockitoBean으로 변경
    private PostService postService;

    @Test
    @DisplayName("User 1: 게시글 생성 API 테스트")
    void createPostTest() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest();
        request.setUserId(1L);
        request.setTitle("테스트 제목");
        request.setContent("테스트 내용");

        doNothing().when(postService).createPostWithImages(any(PostCreateRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("User 1: 게시글 수정 API 테스트")
    void updatePostTest() throws Exception {
        // given
        Long postId = 100L;
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("수정된 제목");
        
        doNothing().when(postService).updatePost(eq(postId), any(PostUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("User 1: 게시글 삭제 API 테스트")
    void deletePostTest() throws Exception {
        // given
        Long postId = 100L;
        doNothing().when(postService).deletePost(postId);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/{postId}", postId))
                .andExpect(status().isNoContent());
    }
}