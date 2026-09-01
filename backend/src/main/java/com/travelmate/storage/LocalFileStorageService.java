package com.travelmate.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadRoot;

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, String extension) throws IOException {
        String objectKey = StorageObjectNames.newImageKey(extension);
        String relativeName = objectKey.substring("uploads/".length());
        Path destination = uploadRoot.resolve(relativeName).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IOException("Invalid storage path");
        }
        Files.createDirectories(destination.getParent());
        file.transferTo(destination);
        return new StoredFile(objectKey, "/" + objectKey);
    }

    @Override
    public void delete(String objectKey) throws IOException {
        Files.deleteIfExists(resolve(objectKey));
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public String publicUrl(String objectKey) {
        return "/" + StorageObjectNames.requireSafeKey(objectKey);
    }

    private Path resolve(String objectKey) {
        String safeKey = StorageObjectNames.requireSafeKey(objectKey);
        Path destination = uploadRoot.resolve(safeKey.substring("uploads/".length())).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid storage object key");
        }
        return destination;
    }
}
