package com.travelmate.storage;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

final class StorageObjectNames {

    private StorageObjectNames() {
    }

    static String newImageKey(String extension) {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return "uploads/%04d/%02d/%s".formatted(date.getYear(), date.getMonthValue(), fileName);
    }

    static String requireSafeKey(String objectKey) {
        if (objectKey == null || !objectKey.startsWith("uploads/")
                || objectKey.contains("..") || objectKey.contains("\\")
                || objectKey.startsWith("/") || objectKey.endsWith("/")) {
            throw new IllegalArgumentException("Invalid storage object key");
        }
        return objectKey;
    }
}
