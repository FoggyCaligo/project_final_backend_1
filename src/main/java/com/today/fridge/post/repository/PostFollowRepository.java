package com.today.fridge.post.repository;

import com.today.fridge.post.entity.PostFollow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostFollowRepository extends JpaRepository<PostFollow, Long> {
    
    // 특정 관계가 존재하는지 확인
    boolean existsByFollowerUser_UserIdAndFolloweeUser_UserId(Long followerId, Long followeeId);
    
    // 특정 관계 삭제
    void deleteByFollowerUser_UserIdAndFolloweeUser_UserId(Long followerId, Long followeeId);
}