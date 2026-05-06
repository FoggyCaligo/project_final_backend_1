package com.today.fridge.bookmark.service;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.entity.Bookmark;
import com.today.fridge.bookmark.repository.BookmarkRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository; // 💡 추가
    private final RecipeRepository recipeRepository; // 💡 추가

    public List<BookmarkedRecipeResponse> getBookmarkedRecipes(Long userId) {
        return bookmarkRepository.findBookmarkedRecipesByUserId(userId);
    }

    // 💡 추가: 북마크 등록
    @Transactional
    public void addBookmark(Long userId, Long recipeId) {
        if (!bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(userId, recipeId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다."));

            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setRecipe(recipe);
            bookmark.setCreatedAt(LocalDateTime.now());
            bookmarkRepository.save(bookmark);
        }
    }

    // 💡 추가: 북마크 취소
    @Transactional
    public void removeBookmark(Long userId, Long recipeId) {
        bookmarkRepository.deleteByUser_UserIdAndRecipe_RecipeId(userId, recipeId);
    }

    // 💡 추가: 북마크 상태 확인
    public boolean checkBookmarkStatus(Long userId, Long recipeId) {
        return bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(userId, recipeId);
    }
}