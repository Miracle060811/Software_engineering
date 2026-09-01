package com.travelmate;

import com.travelmate.common.Result;
import com.travelmate.controller.FileController;
import com.travelmate.storage.LocalFileStorageService;
import com.travelmate.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileControllerTests {

    @TempDir
    Path uploadDirectory;

    @Test
    void rejectsEmptyFile() {
        FileController controller = controller();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]);

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("为空"));
    }

    @Test
    void rejectsOversizedFile() {
        FileController controller = controller();
        byte[] payload = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", payload);

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("过大"));
    }

    @Test
    void rejectsUnsupportedExtension() {
        FileController controller = controller();
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.exe", "application/octet-stream", "binary".getBytes());

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不支持的文件类型"));
    }

    @Test
    void rejectsUnsupportedContentType() {
        FileController controller = controller();
        byte[] jpegHeader = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "application/octet-stream", jpegHeader);

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("内容不是受支持的图片格式"));
    }

    @Test
    void rejectsExtensionAndContentMismatch() {
        FileController controller = controller();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "not-an-image".getBytes());

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不匹配"));
    }

    @Test
    void storesValidJpegWithImmutableUuidName() throws Exception {
        FileController controller = controller();
        byte[] jpegHeader = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpegHeader);

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(200, result.getCode());
        String url = result.getData().get("url");
        assertTrue(url.matches("/uploads/\\d{4}/\\d{2}/[a-f0-9]{32}\\.jpg"));
        assertEquals(1, countStoredFiles());
    }

    @Test
    void storesValidWebpWithImmutableUuidName() throws Exception {
        FileController controller = controller();
        byte[] webpHeader = new byte[] {
                'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.webp", "image/webp", webpHeader);

        Result<Map<String, String>> result = controller.upload(file);

        assertEquals(200, result.getCode());
        String url = result.getData().get("url");
        assertTrue(url.matches("/uploads/\\d{4}/\\d{2}/[a-f0-9]{32}\\.webp"));
        assertEquals(1, countStoredFiles());
    }

    @Test
    void localStorageCanCheckAndDeleteObject() throws Exception {
        LocalFileStorageService storage = new LocalFileStorageService(uploadDirectory.toString());
        byte[] jpegHeader = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
        StoredFile stored = storage.store(new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpegHeader), "jpg");

        assertTrue(storage.exists(stored.objectKey()));
        assertEquals("/" + stored.objectKey(), storage.publicUrl(stored.objectKey()));

        storage.delete(stored.objectKey());
        assertFalse(storage.exists(stored.objectKey()));
    }

    @Test
    void localStorageRejectsPathTraversalKeys() {
        LocalFileStorageService storage = new LocalFileStorageService(uploadDirectory.toString());

        assertThrows(IllegalArgumentException.class, () -> storage.exists("uploads/../../secret.txt"));
        assertThrows(IllegalArgumentException.class, () -> storage.publicUrl("other/file.jpg"));
    }

    private FileController controller() {
        return new FileController(new LocalFileStorageService(uploadDirectory.toString()));
    }

    private long countStoredFiles() throws IOException {
        try (var files = Files.walk(uploadDirectory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}
