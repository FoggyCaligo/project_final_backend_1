package com.today.fridge.post.repository;

import com.today.fridge.post.dto.PostSummaryResponse;
import com.today.fridge.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT new com.today.fridge.post.dto.PostSummaryResponse(" +
           "p.title, u.loginId, p.createdAt, p.content, f.storagePath, f.storedName) " +
           "FROM Post p " +
           "JOIN p.authorUser u " +
           "LEFT JOIN PostImage pi ON pi.post.postId = p.postId AND pi.sortOrder = 1 " + // 첫 번째 이미지만 가져옴
           "LEFT JOIN pi.file f " +
           "WHERE u.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    List<PostSummaryResponse> findRecentPostsByUserId(@Param("userId") Long userId);
}