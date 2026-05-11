package com.today.fridge.bookmark.service;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.entity.Bookmark;
import com.today.fridge.bookmark.repository.BookmarkRepository;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public List<BookmarkedRecipeResponse> getBookmarkedRecipes(Long userId) {
        Long resolvedUserId = resolveUserId(userId);
        return bookmarkRepository.findBookmarkedRecipesByUserId(resolvedUserId);
    }

    @Transactional
    public void addBookmark(Long userId, Long recipeId) {
        Long resolvedUserId = resolveUserId(userId);

        if (!bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(resolvedUserId, recipeId)) {
            User user = userRepository.findById(resolvedUserId)
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

    @Transactional
    public void removeBookmark(Long userId, Long recipeId) {
        Long resolvedUserId = resolveUserId(userId);
        bookmarkRepository.deleteByUser_UserIdAndRecipe_RecipeId(resolvedUserId, recipeId);
    }

    public boolean checkBookmarkStatus(Long userId, Long recipeId) {
        Long resolvedUserId = resolveUserId(userId);
        return bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(resolvedUserId, recipeId);
    }

    private Long resolveUserId(Long userId) {
        if (userId != null) {
            return userId;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByLoginId(auth.getName())
                    .map(User::getUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
