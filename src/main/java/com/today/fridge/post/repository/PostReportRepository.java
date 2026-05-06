package com.today.fridge.post.repository;

import com.today.fridge.post.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    // 💡 특정 유저가 특정 게시글을 신고했는지 여부 확인
    boolean existsByPost_PostIdAndReporterUser_UserId(Long postId, Long reporterUserId);
}