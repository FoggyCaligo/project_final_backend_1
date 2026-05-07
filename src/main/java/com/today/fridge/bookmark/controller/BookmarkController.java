package com.today.fridge.bookmark.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Bookmark", description = "BookmarkController API")
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // 💡 변경: @PathVariable("userId") 명시
    @GetMapping("/{userId}")
    @Operation(summary = "Bookmark API")
    public ResponseEntity<List<BookmarkedRecipeResponse>> getBookmarksByUser(@Parameter(description = "userId") @PathVariable("userId") Long userId) {
        List<BookmarkedRecipeResponse> response = bookmarkService.getBookmarkedRecipes(userId);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @PathVariable("recipeId"), @RequestParam("userId") 명시
    @PostMapping("/{recipeId}")
    @Operation(summary = "Bookmark API")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @Parameter(description = "recipeId") @PathVariable("recipeId") Long recipeId, 
            @RequestParam("userId") Long userId) {
        
        bookmarkService.addBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @PathVariable("recipeId"), @RequestParam("userId") 명시
    @DeleteMapping("/{recipeId}")
    @Operation(summary = "Bookmark API")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @Parameter(description = "recipeId") @PathVariable("recipeId") Long recipeId, 
            @RequestParam("userId") Long userId) {
        
        bookmarkService.removeBookmark(userId, recipeId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 💡 변경: @PathVariable("recipeId"), @RequestParam("userId") 명시
    @GetMapping("/{recipeId}/status")
    @Operation(summary = "Bookmark API")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @Parameter(description = "recipeId") @PathVariable("recipeId") Long recipeId, 
            @RequestParam("userId") Long userId) {
        
        boolean isBookmarked = bookmarkService.checkBookmarkStatus(userId, recipeId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isBookmarked", isBookmarked);
        return ResponseEntity.ok(response);
    }
}