package com.today.fridge.bookmark.controller;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<BookmarkedRecipeResponse>> getBookmarksByUser(@PathVariable Long userId) {
        
        List<BookmarkedRecipeResponse> response = bookmarkService.getBookmarkedRecipes(userId);
        
        return ResponseEntity.ok(response);
    }
}