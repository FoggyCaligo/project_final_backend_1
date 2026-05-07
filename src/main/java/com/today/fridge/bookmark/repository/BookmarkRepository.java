package com.today.fridge.bookmark.repository;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("SELECT new com.today.fridge.bookmark.dto.BookmarkedRecipeResponse(" +
           "r.recipeId, r.title, r.thumbnailUrl, r.summary, r.servingsText, r.cookTimeText, r.difficultyLevel" +
           ") " +
           "FROM Bookmark b JOIN b.recipe r " +
           "WHERE b.user.userId = :userId")
    List<BookmarkedRecipeResponse> findBookmarkedRecipesByUserId(@Param("userId") Long userId);

    boolean existsByUser_UserIdAndRecipe_RecipeId(Long userId, Long recipeId);

    void deleteByUser_UserIdAndRecipe_RecipeId(Long userId, Long recipeId);
}
