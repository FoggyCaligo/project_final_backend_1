package com.today.fridge.global.upload;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 이미 읽은 바이트로 MultipartFile을 재구성해 스트림 재사용 문제를 피한다.
 */
public class BytesMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public BytesMultipartFile(byte[] content, String originalFilename, String contentType) {
        this.content = content != null ? content : new byte[0];
        this.originalFilename = originalFilename != null ? originalFilename : "upload.jpg";
        this.name = "file";
        this.contentType = contentType != null ? contentType : "application/octet-stream";
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    @NonNull
    public byte[] getBytes() {
        return content;
    }

    @Override
    @NonNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(@NonNull File dest) throws IOException {
        java.nio.file.Files.write(dest.toPath(), content);
    }
}
