package com.travelmate.controller;

import com.travelmate.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            return Result.error("文件为空");

        if (file.getSize() > MAX_SIZE)
            return Result.error("文件过大，最大10MB");

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext))
            return Result.error("不支持的文件类型，仅允许: " + String.join(",", ALLOWED_EXTENSIONS));

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT)))
            return Result.error("文件内容不是受支持的图片格式");

        try {
            if (!hasValidImageSignature(file, ext))
                return Result.error("图片文件内容与扩展名不匹配");
        } catch (IOException e) {
            return Result.error("无法读取上传文件");
        }

        String newName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            File dir = new File(uploadDir).getCanonicalFile();
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.error("上传目录创建失败");
            }
            File dest = new File(dir, newName);
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }

        String url = "/uploads/" + newName;
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("name", originalName);
        return Result.success(data);
    }

    private boolean hasValidImageSignature(MultipartFile file, String extension) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (extension) {
                case "jpg", "jpeg" -> header.length >= 3
                        && unsigned(header[0]) == 0xff && unsigned(header[1]) == 0xd8 && unsigned(header[2]) == 0xff;
                case "png" -> header.length >= 8
                        && unsigned(header[0]) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                        && unsigned(header[4]) == 0x0d && unsigned(header[5]) == 0x0a
                        && unsigned(header[6]) == 0x1a && unsigned(header[7]) == 0x0a;
                case "gif" -> header.length >= 6
                        && header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                        && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a';
                case "webp" -> header.length >= 12
                        && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
        }
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
