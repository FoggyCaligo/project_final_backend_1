package com.today.fridge.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostImageDto {
    private String storagePath;
    private String storedName;
}