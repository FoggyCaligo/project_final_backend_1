package com.today.fridge.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.today.fridge.post.service.PostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "PostReport", description = "PostReportController API")
@RequestMapping("/api/v1/posts/{postId}/reports")
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportService postReportService;

    // 💡 1. 신고하기 (POST)
    @PostMapping
    @Operation(summary = "PostReport API")
    public ResponseEntity<Map<String, Object>> reportPost(
            @Parameter(description = "postId") @PathVariable("postId") Long postId,
            @Parameter(description = "userId") @RequestParam("userId") Long userId,
            @Parameter(description = "reasonCode") @RequestParam(value = "reasonCode", required = false) String reasonCode,
            @Parameter(description = "detailText") @RequestParam(value = "detailText", required = false) String detailText) {

        postReportService.addReport(postId, userId, reasonCode, detailText);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // 💡 2. 신고 여부 조회 (GET)
    @GetMapping("/status")
    @Operation(summary = "PostReport API")
    public ResponseEntity<Map<String, Boolean>> getReportStatus(
            @Parameter(description = "postId") @PathVariable("postId") Long postId,
            @Parameter(description = "userId") @RequestParam("userId") Long userId) {

        boolean isReported = postReportService.checkReportStatus(postId, userId);

        Map<String, Boolean> response = new HashMap<>();
        response.put("isReported", isReported);
        return ResponseEntity.ok(response);
    }
}