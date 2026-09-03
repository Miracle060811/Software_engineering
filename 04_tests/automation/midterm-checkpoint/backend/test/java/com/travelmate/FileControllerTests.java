package com.travelmate;

import com.travelmate.common.Result;
import com.travelmate.controller.FileController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(url.matches("/uploads/[a-f0-9]{32}\\.jpg"));
        try (var files = Files.list(uploadDirectory)) {
            assertEquals(1, files.count());
        }
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
        assertTrue(url.matches("/uploads/[a-f0-9]{32}\\.webp"));
        try (var files = Files.list(uploadDirectory)) {
            assertEquals(1, files.count());
        }
    }

    private FileController controller() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", uploadDirectory.toString());
        return controller;
    }
}
