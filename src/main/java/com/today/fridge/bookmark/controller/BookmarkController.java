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

    @GetMapping
    public ResponseEntity<List<BookmarkedRecipeResponse>> getBookmarksByUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<BookmarkedRecipeResponse> response = bookmarkService.getBookmarkedRecipes(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{recipeId}")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("recipeId") Long recipeId) {
        
        bookmarkService.addBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("recipeId") Long recipeId) {
        
        bookmarkService.removeBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{recipeId}/status")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("recipeId") Long recipeId) {
        
        boolean isBookmarked = bookmarkService.checkBookmarkStatus(userId, recipeId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isBookmarked", isBookmarked);
        return ResponseEntity.ok(response);
    }
}
