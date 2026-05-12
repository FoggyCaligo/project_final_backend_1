package com.today.fridge.vision.dto;

import java.util.Optional;

/**
 * 비전 이미지 DB·디스크 저장 결과 — 클라이언트는 {@link #status()}로 성공·실패·스킵을 구분한다.
 */
public record VisionPersistResult(
        Optional<Long> recognitionRequestId,
        Optional<Long> fileAssetId,
        VisionPersistStatus status) {

    public static VisionPersistResult skipped() {
        return new VisionPersistResult(Optional.empty(), Optional.empty(), VisionPersistStatus.SKIPPED);
    }

    public static VisionPersistResult failed() {
        return new VisionPersistResult(Optional.empty(), Optional.empty(), VisionPersistStatus.FAILED);
    }

    public static VisionPersistResult success(long fileAssetId, long recognitionRequestId) {
        return new VisionPersistResult(
                Optional.of(recognitionRequestId), Optional.of(fileAssetId), VisionPersistStatus.SUCCESS);
    }

    public enum VisionPersistStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
