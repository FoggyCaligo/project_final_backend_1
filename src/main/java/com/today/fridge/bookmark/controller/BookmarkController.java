package com.today.fridge.bookmark.controller;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    
    // 💡 주의: 이전에 있던 private final Long currentUserId = 1L; 은 완전히 삭제되어야 합니다!

    @GetMapping("/{userId}")
    public ResponseEntity<List<BookmarkedRecipeResponse>> getBookmarksByUser(@PathVariable Long userId) {
        List<BookmarkedRecipeResponse> response = bookmarkService.getBookmarkedRecipes(userId);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @RequestParam으로 프론트엔드가 보낸 userId를 받아서 Service로 넘깁니다.
    @PostMapping("/{recipeId}")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @PathVariable Long recipeId, 
            @RequestParam Long userId) { // <-- 프론트엔드의 ?userId=2 값을 여기서 받음
        
        bookmarkService.addBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @PathVariable Long recipeId, 
            @RequestParam Long userId) {
        
        bookmarkService.removeBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{recipeId}/status")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @PathVariable Long recipeId, 
            @RequestParam Long userId) {
        
        boolean isBookmarked = bookmarkService.checkBookmarkStatus(userId, recipeId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isBookmarked", isBookmarked);
        return ResponseEntity.ok(response);
    }
}