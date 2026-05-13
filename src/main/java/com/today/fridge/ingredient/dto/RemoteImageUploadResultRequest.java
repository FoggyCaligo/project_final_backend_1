package com.today.fridge.ingredient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** 클라이언트가 아파치 업로드 결과를 Spring에 알릴 때 (DB 스키마 변경 없음). */
@Getter
@Setter
public class RemoteImageUploadResultRequest {

    private Boolean success;

    /** PHP 응답 참고용 — DB 컬럼 없음, 로깅·확장용으로만 수신 가능 */
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
