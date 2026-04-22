package com.today.fridge.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.post.entity.Post;

public interface PostRepository extends JpaRepository<Post,Long> {

}
