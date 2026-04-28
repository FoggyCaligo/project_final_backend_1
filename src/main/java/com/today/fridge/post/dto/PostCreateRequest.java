package com.today.fridge.post.dto;

import com.today.fridge.file.dto.FileAssetDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PostCreateRequest {
    private Long userId; // 프론트에서 임시로 보내는 작성자 ID
    private String title;
    private String content;
    private Long recipe;
    
    @JsonProperty("image_files")
    private List<FileAssetDto> imageFiles;
}