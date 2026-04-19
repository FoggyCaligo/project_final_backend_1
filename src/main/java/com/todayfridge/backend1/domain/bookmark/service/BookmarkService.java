package com.todayfridge.backend1.domain.bookmark.service;

import com.todayfridge.backend1.domain.bookmark.entity.Bookmark;
import com.todayfridge.backend1.domain.bookmark.repository.BookmarkRepository;
import com.todayfridge.backend1.domain.recipe.entity.Recipe;
import com.todayfridge.backend1.domain.recipe.repository.RecipeRepository;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.domain.user.repository.UserRepository;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, UserRepository userRepository, RecipeRepository recipeRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long userId) {
        return bookmarkRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(bookmark -> Map.of(
                        "bookmarkId", bookmark.getId(),
                        "recipeId", bookmark.getRecipe().getId(),
                        "title", bookmark.getRecipe().getTitle(),
                        "thumbnailUrl", bookmark.getRecipe().getThumbnailUrl()
                )).toList();
    }

    public void add(Long userId, Long recipeId) {
        if (bookmarkRepository.existsByUser_IdAndRecipe_Id(userId, recipeId)) return;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다."));
        bookmarkRepository.save(new Bookmark(user, recipe));
    }

    public void remove(Long userId, Long recipeId) {
        bookmarkRepository.deleteByUser_IdAndRecipe_Id(userId, recipeId);
    }
}
