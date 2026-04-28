package com.today.fridge.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileAssetDto {
    @JsonProperty("original_name")
    private String originalName;
    
    @JsonProperty("uuid_name")
    private String uuidName;
    
    @JsonProperty("sha1sum")
    private String sha1sum;
    
    @JsonProperty("mime_type")
    private String mimeType;
    
    @JsonProperty("file_size")
    private Long fileSize;
    
    @JsonProperty("storage_path")
    private String storagePath;
}