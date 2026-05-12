package com.today.fridge.vision.service;

import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.file.repository.FileAssetRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 비전 업로드: {@code file_asset} 행을 먼저 두고(발급된 {@code file_id} 기준) {@code uploads/vision/{file_id}.{ext}}에 바이트를 쓴다.
 * 호출부 트랜잭션과 합류({@link Propagation#REQUIRED})하여 이후 단계 실패 시 DB 롤백과 함께 정리한다.
 */
@Service
@RequiredArgsConstructor
public class VisionFileAssetService {

    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.root-path:uploads}")
    private String uploadRoot;

    /**
     * DB 행 삽입(경로·파일명은 flush 후 {@code file_id} 기준으로 확정) → 디스크 기록 → 메타 갱신.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public FileAsset insertRowAndWriteVisionFile(
            Long userId, byte[] imageBytes, String originalFilename, String contentType) throws IOException {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        String ext = extensionFromFilename(originalFilename);

        FileAsset fa = new FileAsset();
        fa.setUploaderUser(user);
        fa.setStorageType("LOCAL");
        fa.setOriginalName(originalFilename != null ? originalFilename : "upload.jpg");
        fa.setStoredName("pending");
        fa.setMimeType(contentType);
        fa.setFileSize((long) imageBytes.length);
        fa.setStoragePath(null);
        fa.setCreatedAt(LocalDateTime.now());
        fileAssetRepository.save(fa);
        fileAssetRepository.flush();

        Long id = fa.getFileId();
        String stored = id + ext;
        String relPath = "vision/" + stored;
        fa.setStoredName(stored);
        fa.setStoragePath(relPath);

        Path base = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path visionDir = base.resolve("vision");
        Files.createDirectories(visionDir);
        Path target = visionDir.resolve(stored);
        Files.write(target, imageBytes);

        return fileAssetRepository.save(fa);
    }

    /** {@code storage_path} 기준 업로드 루트 아래 절대 경로 (롤백 시 디스크 삭제용). */
    public Path absolutePathForStoredFile(FileAsset fa) {
        if (fa == null || fa.getStoragePath() == null || fa.getStoragePath().isBlank()) {
            return null;
        }
        Path base = Paths.get(uploadRoot).toAbsolutePath().normalize();
        return base.resolve(fa.getStoragePath()).normalize();
    }

    public void deletePhysicalFileQuietly(Path absolutePath) {
        if (absolutePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(absolutePath);
        } catch (IOException ignored) {
            // best-effort
        }
    }

    private static String extensionFromFilename(String name) {
        if (name == null || !name.contains(".")) {
            return ".jpg";
        }
        String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
        if (ext.length() > 10) {
            return ".jpg";
        }
        return ext;
    }
}
