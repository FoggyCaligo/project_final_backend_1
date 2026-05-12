package com.today.fridge.ingredient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.today.fridge.file.dto.FileAssetDto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateIngredientRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal quantity;

    @Size(max = 20)
    private String unit;

    @Size(max = 30)
    private String storageType;

    private LocalDate expirationDate;

    private Long categoryId;

    /** 아파치 등 업로드 후 전달되는 파일 메타데이터(게시글 {@code image_files}와 동일 스키마). */
    @JsonProperty("image_file")
    private FileAssetDto imageFile;

    /**
     * 이미지 인식 API에서 발급된 {@code file_asset.file_id} — 비전 업로드 분과 동일 파일을 식재료에 연결할 때 사용.
     * {@link #imageFile} 과 동시에 오면 {@code file_id}가 우선한다.
     */
    @JsonProperty("file_id")
    @JsonAlias({"fileId"})
    private Long fileId;

    /**
     * true 이면 아파치 업로드 전 DB에만 {@code file_asset} + 연결을 만들고, 클라이언트가 PHP 업로드 후
     * {@code PATCH .../apache-image-sync} 로 성공/실패를 알린다. {@link #fileId} 와 동시 사용 불가.
     */
    @JsonProperty("remote_image_pending")
    @JsonAlias({"remoteImagePending"})
    private Boolean remoteImagePending;

    @JsonProperty("remote_image_original_name")
    @JsonAlias({"remoteImageOriginalName"})
    private String remoteImageOriginalName;

    @JsonProperty("remote_image_mime_type")
    @JsonAlias({"remoteImageMimeType"})
    private String remoteImageMimeType;

    @JsonProperty("remote_image_size")
    @JsonAlias({"remoteImageSize"})
    private Long remoteImageSize;
}
