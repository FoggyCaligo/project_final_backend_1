package com.today.fridge.post.controller;

import com.today.fridge.post.service.PostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts/{postId}/reports")
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportService postReportService;

    // 💡 1. 신고하기 (POST)
    @PostMapping
    public ResponseEntity<Map<String, Object>> reportPost(
            @PathVariable("postId") Long postId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "reasonCode", required = false) String reasonCode,
            @RequestParam(value = "detailText", required = false) String detailText) {

        postReportService.addReport(postId, userId, reasonCode, detailText);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 💡 2. 신고 여부 조회 (GET)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getReportStatus(
            @PathVariable("postId") Long postId,
            @RequestParam("userId") Long userId) {

        boolean isReported = postReportService.checkReportStatus(postId, userId);

        Map<String, Boolean> response = new HashMap<>();
        response.put("isReported", isReported);
        return ResponseEntity.ok(response);
    }
}