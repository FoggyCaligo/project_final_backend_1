package com.todayfridge.backend1.domain.bookmark.controller;

import com.todayfridge.backend1.domain.bookmark.service.BookmarkService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(bookmarkService.list(loginUser.getUserId()));
    }

    @PostMapping("/{recipeId}")
    public ApiResponse<Void> add(@CurrentUser LoginUser loginUser, @PathVariable Long recipeId) {
        bookmarkService.add(loginUser.getUserId(), recipeId);
        return ApiResponse.success("북마크에 추가했습니다.", null);
    }

    @DeleteMapping("/{recipeId}")
    public ApiResponse<Void> remove(@CurrentUser LoginUser loginUser, @PathVariable Long recipeId) {
        bookmarkService.remove(loginUser.getUserId(), recipeId);
        return ApiResponse.success("북마크를 삭제했습니다.", null);
    }
}
