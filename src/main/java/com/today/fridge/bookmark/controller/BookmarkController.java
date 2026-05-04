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

    // 💡 변경: @PathVariable("userId") 명시
    @GetMapping("/{userId}")
    public ResponseEntity<List<BookmarkedRecipeResponse>> getBookmarksByUser(@PathVariable("userId") Long userId) {
        List<BookmarkedRecipeResponse> response = bookmarkService.getBookmarkedRecipes(userId);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @PathVariable("recipeId"), @RequestParam("userId") 명시
    @PostMapping("/{recipeId}")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @PathVariable("recipeId") Long recipeId, 
            @RequestParam("userId") Long userId) {
        
        bookmarkService.addBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @PathVariable("recipeId"), @RequestParam("userId") 명시
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @PathVariable("recipeId") Long recipeId, 
            @RequestParam("userId") Long userId) {
        
        bookmarkService.removeBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @PathVariable("recipeId"), @RequestParam("userId") 명시
    @GetMapping("/{recipeId}/status")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @PathVariable("recipeId") Long recipeId, 
            @RequestParam("userId") Long userId) {
        
        boolean isBookmarked = bookmarkService.checkBookmarkStatus(userId, recipeId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isBookmarked", isBookmarked);
        return ResponseEntity.ok(response);
    }
}