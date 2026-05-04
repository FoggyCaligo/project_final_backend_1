package com.today.fridge.post.service;

import com.today.fridge.post.entity.Post;
import com.today.fridge.post.entity.PostReport;
import com.today.fridge.post.repository.PostReportRepository;
import com.today.fridge.post.repository.PostRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostReportService {

    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addReport(Long postId, Long reporterUserId, String reasonCode, String detailText) {
        // 이미 신고한 유저인지 검증
        if (postReportRepository.existsByPost_PostIdAndReporterUser_UserId(postId, reporterUserId)) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. post_report 이력 저장
        PostReport postReport = new PostReport();
        postReport.setPost(post);
        postReport.setReporterUser(reporter);
        postReport.setReasonCode(reasonCode);
        postReport.setDetailText(detailText);
        postReportRepository.save(postReport);

        // 2. posts 테이블의 report_count 1 증가
        long currentCount = post.getReportCount() == null ? 0L : post.getReportCount();
        post.setReportCount(currentCount + 1);
    }

    @Transactional(readOnly = true)
    public boolean checkReportStatus(Long postId, Long reporterUserId) {
        return postReportRepository.existsByPost_PostIdAndReporterUser_UserId(postId, reporterUserId);
    }
}