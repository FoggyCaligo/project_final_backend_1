package com.today.fridge.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.post.entity.PostReport;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

}
