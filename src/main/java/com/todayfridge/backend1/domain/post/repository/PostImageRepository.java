package com.todayfridge.backend1.domain.post.repository;

import com.todayfridge.backend1.domain.post.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByPost_Id(Long postId);
}
