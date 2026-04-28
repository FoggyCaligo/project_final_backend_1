package com.today.fridge.bookmark.service;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    public List<BookmarkedRecipeResponse> getBookmarkedRecipes(Long userId) {
        return bookmarkRepository.findBookmarkedRecipesByUserId(userId);
    }
}