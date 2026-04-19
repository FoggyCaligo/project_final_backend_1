package com.todayfridge.backend1.domain.bookmark.repository;

import com.todayfridge.backend1.domain.bookmark.entity.Bookmark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUser_IdOrderByCreatedAtDesc(Long userId);
    boolean existsByUser_IdAndRecipe_Id(Long userId, Long recipeId);
    void deleteByUser_IdAndRecipe_Id(Long userId, Long recipeId);
}
