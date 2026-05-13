package com.today.fridge.ingredient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoteImageStagingResultRequest {

    @JsonProperty("pending_file_id")
    @JsonAlias({"pendingFileId"})
    private Long pendingFileId;

    private Boolean success;

    @JsonProperty("uploaded_count")
    @JsonAlias({"uploadedCount"})
    private Integer uploadedCount;

    @JsonProperty("sha1sum")
    @JsonAlias({"sha1Sum"})
    private String sha1sum;

    @JsonProperty("mime_type")
    @JsonAlias({"mimeType"})
    private String mimeType;

    @JsonProperty("file_size")
    @JsonAlias({"fileSize"})
    private Long fileSize;
}
