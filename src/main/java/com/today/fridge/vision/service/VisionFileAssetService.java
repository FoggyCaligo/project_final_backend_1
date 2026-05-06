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
import java.util.UUID;

/**
 * 비전 업로드 바이트를 디스크 + {@code file_asset}에 저장한다.
 * {@link Propagation#REQUIRES_NEW}로 커밋하여, 이후 {@code vision_recognition_request} INSERT 실패 시에도
 * 파일 행이 롤백되지 않게 한다 (프론트에 {@code fileId} 전달·식재료 연동 가능).
 */
@Service
@RequiredArgsConstructor
public class VisionFileAssetService {

    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.root-path:uploads}")
    private String uploadRoot;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public FileAsset saveVisionFileForUser(
            Long userId, byte[] imageBytes, String originalFilename, String contentType)
            throws IOException {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        String ext = extensionFromFilename(originalFilename);
        String stored = UUID.randomUUID() + ext;
        Path base = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path visionDir = base.resolve("vision");
        Files.createDirectories(visionDir);
        Path target = visionDir.resolve(stored);
        Files.write(target, imageBytes);

        FileAsset fa = new FileAsset();
        fa.setUploaderUser(user);
        fa.setStorageType("LOCAL");
        fa.setOriginalName(originalFilename != null ? originalFilename : "upload.jpg");
        fa.setStoredName(stored);
        fa.setMimeType(contentType);
        fa.setFileSize((long) imageBytes.length);
        fa.setStoragePath("vision/" + stored);
        fa.setCreatedAt(LocalDateTime.now());
        return fileAssetRepository.save(fa);
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
