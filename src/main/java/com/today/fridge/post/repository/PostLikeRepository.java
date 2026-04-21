package com.today.fridge.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.post.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

}
