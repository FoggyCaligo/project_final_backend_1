package com.today.fridge.post.dto;

import com.today.fridge.file.dto.FileAssetDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PostUpdateRequest {
    private String title;
    private String content;
    private Long recipe;
    private List<FileAssetDto> image_files; // 💡 새로 추가된 이미지 정보
    private List<String> retained_images;   // 💡 살아남은 기존 이미지들의 저장 파일명(storedName) 목록
}