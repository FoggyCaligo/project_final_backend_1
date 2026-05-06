package com.today.fridge.bookmark.repository;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("SELECT new com.today.fridge.bookmark.dto.BookmarkedRecipeResponse(r.recipeId, r.title) " +
           "FROM Bookmark b JOIN b.recipe r " +
           "WHERE b.user.userId = :userId")
    List<BookmarkedRecipeResponse> findBookmarkedRecipesByUserId(@Param("userId") Long userId);

    // 💡 추가: 특정 유저와 레시피의 북마크 존재 여부 확인
    boolean existsByUser_UserIdAndRecipe_RecipeId(Long userId, Long recipeId);

    // 💡 추가: 특정 유저와 레시피의 북마크 삭제
    void deleteByUser_UserIdAndRecipe_RecipeId(Long userId, Long recipeId);
}