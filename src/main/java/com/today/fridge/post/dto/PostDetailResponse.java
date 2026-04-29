package com.today.fridge.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PostDetailResponse {
    private Long postId;
    private String title;
    private String content;
    private String authorLoginId;
    private Long authorUserId; // 본인 확인용
    private LocalDateTime createdAt;
    private Long recipeId;
    private List<PostImageDto> images; // 이미지 목록 (storagePath, storedName 포함)
}