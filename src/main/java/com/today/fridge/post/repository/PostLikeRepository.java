package com.today.fridge.post.repository;

import com.today.fridge.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPost_PostIdAndUser_UserId(Long postId, Long userId);
    void deleteByPost_PostIdAndUser_UserId(Long postId, Long userId);
}