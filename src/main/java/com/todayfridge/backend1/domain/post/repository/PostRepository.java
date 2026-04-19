package com.todayfridge.backend1.domain.post.repository;

import com.todayfridge.backend1.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
