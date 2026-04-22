package com.today.fridge.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.file.entity.FileAsset;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

}
