package com.todayfridge.backend1.global.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public final class FileUtils {
    private FileUtils() {}

    public static String sanitizeOriginalFilename(String name) {
        if (name == null || name.isBlank()) return "unknown";
        return name.replaceAll("[\\/:*?\"<>|]", "_");
    }

    public static String extensionOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public static String createStoredFilename(String extension) {
        return UUID.randomUUID() + (extension == null || extension.isBlank() ? "" : "." + extension.toLowerCase());
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 생성 실패", e);
        }
    }
}
