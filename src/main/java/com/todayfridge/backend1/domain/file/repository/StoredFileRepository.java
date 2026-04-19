package com.todayfridge.backend1.domain.file.repository;

import com.todayfridge.backend1.domain.file.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
