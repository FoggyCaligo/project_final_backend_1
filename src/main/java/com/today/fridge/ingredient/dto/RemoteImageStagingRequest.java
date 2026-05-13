package com.today.fridge.ingredient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoteImageStagingRequest {

    @JsonProperty("original_name")
    @JsonAlias({"originalName"})
    private String originalName;

    @JsonProperty("mime_type")
    @JsonAlias({"mimeType"})
    private String mimeType;

    @JsonProperty("file_size")
    @JsonAlias({"fileSize"})
    private Long fileSize;
}
