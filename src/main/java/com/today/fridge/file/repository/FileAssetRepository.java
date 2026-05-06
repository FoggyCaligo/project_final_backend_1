package com.today.fridge.file.repository;

import com.today.fridge.file.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    Optional<FileAsset> findByFileIdAndUploaderUser_UserId(Long fileId, Long uploaderUserId);
}
