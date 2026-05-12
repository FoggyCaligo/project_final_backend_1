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

    /**
     * Spring 디스크에는 쓰지 않고 DB만 둔 뒤, 클라이언트가 아파치 {@code upload_fridge_image.php} 로 바이너리를 올린다.
     * {@code vision/{file_id}.확장자} 메타만 채운다.
     */
    @Transactional
    public FileAsset createApachePendingVision(
            User uploader, String originalFilename, String mimeType, Long fileSize) {
        String ext = extensionFromFilenameOrMime(originalFilename, mimeType);
        FileAsset fa = new FileAsset();
        fa.setUploaderUser(uploader);
        fa.setStorageType("REMOTE_APACHE");
        fa.setOriginalName(StringUtils.hasText(originalFilename) ? originalFilename : "upload.jpg");
        fa.setStoredName("pending");
        fa.setMimeType(StringUtils.hasText(mimeType) ? mimeType : "image/jpeg");
        fa.setFileSize(fileSize != null ? fileSize : 0L);
        fa.setStoragePath(null);
        fa.setChecksumValue(null);
        fa.setCreatedAt(LocalDateTime.now());
        fileAssetRepository.save(fa);
        fileAssetRepository.flush();
        Long id = fa.getFileId();
        String stored = id + ext;
        fa.setStoredName(stored);
        fa.setStoragePath("vision/" + stored);
        return fileAssetRepository.save(fa);
    }

    @Transactional
    public void deleteByIdAndUser(Long fileId, Long uploaderUserId) {
        fileAssetRepository.findByFileIdAndUploaderUser_UserId(fileId, uploaderUserId).ifPresent(fileAssetRepository::delete);
    }

    /** 아파치 업로드 성공 후 기존 컬럼만 갱신 (스키마 변경 없음). */
    @Transactional
    public void applyApacheUploadMetadata(
            Long fileId, Long uploaderUserId, String sha1sum, Long fileSize, String mimeType) {
        FileAsset fa = getOwnedFileOrThrow(fileId, uploaderUserId);
        if (StringUtils.hasText(sha1sum)) {
            fa.setChecksumValue(sha1sum);
        }
        if (fileSize != null) {
            fa.setFileSize(fileSize);
        }
        if (StringUtils.hasText(mimeType)) {
            fa.setMimeType(mimeType);
        }
        fileAssetRepository.save(fa);
    }

    private static String extensionFromFilenameOrMime(String originalFilename, String mimeType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (ext.length() > 1 && ext.length() <= 10) {
                return ext;
            }
        }
        if (!StringUtils.hasText(mimeType)) {
            return ".jpg";
        }
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/jpeg", "image/jpg" -> ".jpg";
            default -> ".jpg";
        };
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
