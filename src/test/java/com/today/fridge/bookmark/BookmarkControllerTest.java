package com.today.fridge.bookmark.controller;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.service.BookmarkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class BookmarkControllerTest {

    @Mock
    private BookmarkService bookmarkService;

    private MockMvc mockMvc() {
        return standaloneSetup(new BookmarkController(bookmarkService)).build();
    }

    @Test
    @DisplayName("GET /api/v1/bookmarks/{userId} 요청 시 사용자의 북마크 목록을 JSON 배열로 반환한다")
    void getBookmarksByUser_returnsBookmarkedRecipes() throws Exception {
        // given
        Long userId = 1L;
        when(bookmarkService.getBookmarkedRecipes(userId)).thenReturn(List.of(
                new BookmarkedRecipeResponse(101L, "김치찌개"),
                new BookmarkedRecipeResponse(102L, "된장찌개")
        ));

        // when & then
        mockMvc().perform(get("/api/v1/bookmarks/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipeId").value(101))
                .andExpect(jsonPath("$[0].recipeName").value("김치찌개"))
                .andExpect(jsonPath("$[1].recipeId").value(102))
                .andExpect(jsonPath("$[1].recipeName").value("된장찌개"));

        verify(bookmarkService).getBookmarkedRecipes(userId);
    }

    @Test
    @DisplayName("POST /api/v1/bookmarks/{recipeId}?userId=... 요청 시 북마크를 등록하고 success=true를 반환한다")
    void addBookmark_returnsSuccessResponse() throws Exception {
        // given
        Long userId = 1L;
        Long recipeId = 10L;

        // when & then
        mockMvc().perform(post("/api/v1/bookmarks/{recipeId}", recipeId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookmarkService).addBookmark(userId, recipeId);
    }

    @Test
    @DisplayName("DELETE /api/v1/bookmarks/{recipeId}?userId=... 요청 시 북마크를 삭제하고 success=true를 반환한다")
    void removeBookmark_returnsSuccessResponse() throws Exception {
        // given
        Long userId = 1L;
        Long recipeId = 10L;

        // when & then
        mockMvc().perform(delete("/api/v1/bookmarks/{recipeId}", recipeId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookmarkService).removeBookmark(userId, recipeId);
    }

    @Test
    @DisplayName("GET /api/v1/bookmarks/{recipeId}/status 요청 시 북마크된 상태를 isBookmarked=true로 반환한다")
    void checkStatus_returnsTrueWhenBookmarked() throws Exception {
        // given
        Long userId = 1L;
        Long recipeId = 10L;
        when(bookmarkService.checkBookmarkStatus(userId, recipeId)).thenReturn(true);

        // when & then
        mockMvc().perform(get("/api/v1/bookmarks/{recipeId}/status", recipeId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBookmarked").value(true));

        verify(bookmarkService).checkBookmarkStatus(userId, recipeId);
    }

    @Test
    @DisplayName("GET /api/v1/bookmarks/{recipeId}/status 요청 시 북마크되지 않은 상태를 isBookmarked=false로 반환한다")
    void checkStatus_returnsFalseWhenNotBookmarked() throws Exception {
        // given
        Long userId = 1L;
        Long recipeId = 10L;
        when(bookmarkService.checkBookmarkStatus(userId, recipeId)).thenReturn(false);

        // when & then
        mockMvc().perform(get("/api/v1/bookmarks/{recipeId}/status", recipeId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBookmarked").value(false));

        verify(bookmarkService).checkBookmarkStatus(userId, recipeId);
    }
}
