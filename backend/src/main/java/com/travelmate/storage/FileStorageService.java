package com.travelmate.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String extension) throws IOException;

    void delete(String objectKey) throws IOException;

    boolean exists(String objectKey) throws IOException;

    String publicUrl(String objectKey);
}
