package com.travelmate.controller;

import com.travelmate.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
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

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String newName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dest = new File(dir, newName);
        try {
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
}
