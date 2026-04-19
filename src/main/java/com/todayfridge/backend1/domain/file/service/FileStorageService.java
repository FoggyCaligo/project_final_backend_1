package com.todayfridge.backend1.domain.file.service;

import com.todayfridge.backend1.domain.file.entity.StoredFile;
import com.todayfridge.backend1.domain.file.repository.StoredFileRepository;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.util.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FileStorageService {
    private final StoredFileRepository storedFileRepository;

    @Value("${app.upload.root-path:uploads}")
    private String rootPath;

    public FileStorageService(StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
    }

    public StoredFile store(User owner, MultipartFile file, String subDirectory) {
        try {
            String extension = FileUtils.extensionOf(file);
            String storedName = FileUtils.createStoredFilename(extension);
            Path dir = Path.of(rootPath, subDirectory);
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            return storedFileRepository.save(new StoredFile(
                    owner,
                    FileUtils.sanitizeOriginalFilename(file.getOriginalFilename()),
                    storedName,
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize(),
                    target.toString()
            ));
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패", e);
        }
    }
}
