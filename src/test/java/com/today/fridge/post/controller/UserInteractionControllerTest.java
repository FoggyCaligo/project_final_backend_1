package com.today.fridge.post.controller;

import com.today.fridge.bookmark.controller.BookmarkController;
import com.today.fridge.bookmark.service.BookmarkService;
import com.today.fridge.post.service.PostFollowService;
import com.today.fridge.post.service.PostLikeService;
import com.today.fridge.post.service.PostReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// 💡 @MockitoBean 임포트
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PostLikeController.class, PostFollowController.class, BookmarkController.class, PostReportController.class})
@SuppressWarnings("null") // 💡 Null type safety 경고 숨김 처리
public class UserInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private PostLikeService postLikeService;
    @MockitoBean private PostFollowService postFollowService;
    @MockitoBean private BookmarkService bookmarkService;
    @MockitoBean private PostReportService postReportService;

    private final Long authorId = 1L; // 작성자 (User 1)
    private final Long actorId = 2L;  // 행위자 (User 2)
    private final Long postId = 100L;
    private final Long recipeId = 200L;

    @Test
    @DisplayName("User 2: User 1의 게시글 좋아요 추가 및 취소 테스트")
    void likeAndUnlikeTest() throws Exception {
        doNothing().when(postLikeService).addLike(postId, actorId);
        doNothing().when(postLikeService).removeLike(postId, actorId);

        // 좋아요 추가
        mockMvc.perform(post("/api/v1/posts/{postId}/likes", postId)
                .param("userId", actorId.toString()))
                .andExpect(status().isOk());

        // 좋아요 취소
        mockMvc.perform(delete("/api/v1/posts/{postId}/likes", postId)
                .param("userId", actorId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User 2: User 1 팔로우 추가 및 취소 테스트")
    void followAndUnfollowTest() throws Exception {
        doNothing().when(postFollowService).addFollow(actorId, authorId);
        doNothing().when(postFollowService).removeFollow(actorId, authorId);

        // 팔로우 추가
        mockMvc.perform(post("/api/v1/users/{userId}/follow", authorId)
                .param("followerId", actorId.toString()))
                .andExpect(status().isOk());

        // 팔로우 취소
        mockMvc.perform(delete("/api/v1/users/{userId}/follow", authorId)
                .param("followerId", actorId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User 2: User 1의 게시글 관련 레시피 북마크 추가 및 취소 테스트")
    void bookmarkAndUnbookmarkTest() throws Exception {
        doNothing().when(bookmarkService).addBookmark(actorId, recipeId);
        doNothing().when(bookmarkService).removeBookmark(actorId, recipeId);

        // 북마크 추가
        mockMvc.perform(post("/api/v1/bookmarks/{recipeId}", recipeId)
                .param("userId", actorId.toString()))
                .andExpect(status().isOk());

        // 북마크 취소
        mockMvc.perform(delete("/api/v1/bookmarks/{recipeId}", recipeId)
                .param("userId", actorId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User 2: User 1의 게시글 신고하기 테스트")
    void reportPostTest() throws Exception {
        doNothing().when(postReportService).addReport(postId, actorId, null, null);

        // 신고하기
        mockMvc.perform(post("/api/v1/posts/{postId}/reports", postId)
                .param("userId", actorId.toString()))
                .andExpect(status().isOk());
    }
}