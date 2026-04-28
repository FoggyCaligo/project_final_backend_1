package com.today.fridge.bookmark.repository;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // b.recipe r 을 통해 조인하고, r.title을 DTO의 두 번째 인자(recipeName)로 넘깁니다.
    @Query("SELECT new com.today.fridge.bookmark.dto.BookmarkedRecipeResponse(r.recipeId, r.title) " +
           "FROM Bookmark b JOIN b.recipe r " +
           "WHERE b.user.userId = :userId")
    List<BookmarkedRecipeResponse> findBookmarkedRecipesByUserId(@Param("userId") Long userId);
}