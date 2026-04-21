package com.today.fridge.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.bookmark.entity.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

}
