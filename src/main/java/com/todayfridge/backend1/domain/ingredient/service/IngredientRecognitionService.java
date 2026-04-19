package com.todayfridge.backend1.domain.ingredient.service;

import com.todayfridge.backend1.domain.file.service.FileValidationService;
import com.todayfridge.backend1.global.external.fastapi.FastApiClient;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IngredientRecognitionService {
    private final FileValidationService fileValidationService;
    private final FastApiClient fastApiClient;

    public IngredientRecognitionService(FileValidationService fileValidationService, FastApiClient fastApiClient) {
        this.fileValidationService = fileValidationService;
        this.fastApiClient = fastApiClient;
    }

    public Map<String, Object> recognize(MultipartFile file) {
        fileValidationService.validateSingleIngredientImage(file);
        return Map.of("candidates", fastApiClient.recognizeIngredient(file).candidates());
    }
}
