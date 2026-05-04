package com.today.fridge.post.repository;

import com.today.fridge.post.entity.Post;
import com.today.fridge.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    
    // 💡 추가된 부분: 특정 Post 객체와 연관된 PostImage 목록을 찾는 메서드
    List<PostImage> findByPost(Post post);

    // 💡 추가: 특정 게시글의 이미지 매핑을 모두 삭제하는 메서드
    void deleteByPost(Post post);
}