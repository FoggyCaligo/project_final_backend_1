package com.today.fridge.ingredient.domain;

import com.today.fridge.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Set;

public final class StorageTypePolicy {

    private static final Set<String> ALLOWED = Set.of(
            "REFRIGERATED", "FROZEN", "ROOM_TEMP", "UNKNOWN");

    private StorageTypePolicy() {
    }

    public static boolean isAllowed(String storageType) {
        return storageType != null && ALLOWED.contains(storageType);
    }

    public static void validateOrThrow(String storageType) {
        if (storageType == null) {
            return;
        }
        if (!ALLOWED.contains(storageType)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STORAGE_TYPE",
                    "보관 방식 값이 올바르지 않습니다.");
        }
    }
}
