package com.today.fridge.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.post.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

}
