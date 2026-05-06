package com.today.fridge.file.service;

import com.today.fridge.file.dto.FileAssetDto;
import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.file.repository.FileAssetRepository;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 아파치(또는 정적 스토리지)에 업로드된 뒤 전달받는 메타데이터로 {@code file_asset} 행을 만든다.
 * {@link com.today.fridge.post.service.PostService}와 동일한 필드 매핑.
 */
@Service
@RequiredArgsConstructor
public class FileAssetService {

    private final FileAssetRepository fileAssetRepository;

    @Transactional
    public FileAsset saveFromUploadMetadata(User uploader, FileAssetDto dto) {
        if (dto == null) {
            return null;
        }
        validateForInsert(dto);
        FileAsset fileAsset = new FileAsset();
        fileAsset.setUploaderUser(uploader);
        fileAsset.setStorageType("LOCAL");
        fileAsset.setOriginalName(dto.getOriginalName());
        fileAsset.setStoredName(dto.getUuidName());
        fileAsset.setMimeType(dto.getMimeType());
        fileAsset.setFileSize(dto.getFileSize());
        fileAsset.setStoragePath(dto.getStoragePath());
        fileAsset.setChecksumValue(dto.getSha1sum());
        fileAsset.setCreatedAt(LocalDateTime.now());
        return fileAssetRepository.save(fileAsset);
    }

    @Transactional(readOnly = true)
    public FileAsset getOwnedFileOrThrow(Long fileId, Long uploaderUserId) {
        return fileAssetRepository
                .findByFileIdAndUploaderUser_UserId(fileId, uploaderUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "이미지 파일을 찾을 수 없습니다."));
    }

    private static void validateForInsert(FileAssetDto dto) {
        if (!StringUtils.hasText(dto.getOriginalName())
                || !StringUtils.hasText(dto.getUuidName())
                || !StringUtils.hasText(dto.getMimeType())
                || dto.getFileSize() == null
                || !StringUtils.hasText(dto.getStoragePath())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "파일 메타데이터가 불완전합니다.");
        }
    }
}
