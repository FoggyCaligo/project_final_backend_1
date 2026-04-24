package com.today.fridge.ingredient.domain;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.ingredient.type.StorageType;

public final class StorageTypePolicy {

    private StorageTypePolicy() {
    }

    public static boolean isAllowed(String storageType) {
        if (storageType == null) {
            return true;
        }
        try {
            StorageType.valueOf(storageType);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void validateOrThrow(String storageType) {
        if (storageType == null) {
            return;
        }
        try {
            StorageType.valueOf(storageType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_STORAGE_TYPE);
        }
    }
}
