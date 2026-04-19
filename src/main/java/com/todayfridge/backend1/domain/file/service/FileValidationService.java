package com.todayfridge.backend1.domain.file.service;

import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    public void validateSingleIngredientImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미지 파일이 필요합니다.");
        if (!ALLOWED.contains(file.getContentType())) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 이미지 형식입니다.");
        if (file.getSize() > 5 * 1024 * 1024L) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "파일 크기는 5MB 이하여야 합니다.");
    }

    public void validatePostImages(List<MultipartFile> files) {
        if (files == null) return;
        if (files.size() > 5) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "게시글 이미지는 최대 5장까지 허용됩니다.");
        for (MultipartFile file : files) validateSingleIngredientImage(file);
    }
}
