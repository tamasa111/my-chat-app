package com.chatapp.server.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path storageDirectory;

    public FileStorageService(@Value("${chat.storage.path:uploads}") String storagePath) {
        this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize storage directory " + storageDirectory, exception);
        }
    }

    public StoredFile store(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(multipartFile.getOriginalFilename(), "upload.bin"));
        String fileId = UUID.randomUUID().toString();
        String storedFilename = fileId + "-" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = storageDirectory.resolve(storedFilename);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredFile(
                fileId,
                originalFilename,
                multipartFile.getSize(),
                Objects.requireNonNullElse(multipartFile.getContentType(), "application/octet-stream"),
                target
        );
    }

    public Resource loadAsResource(String storagePath) {
        return new FileSystemResource(storagePath);
    }

    public void deleteIfExists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException ignored) {
        }
    }

    public record StoredFile(String fileId, String originalFilename, long size, String contentType, Path path) {
    }
}
