package com.travelmate.microservices.ops;

import com.travelmate.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/secrets")
public class AdminSecretController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final OpsK8sSecretService service;
    private final String secretName;
    private final String configMapName;

    public AdminSecretController(OpsK8sSecretService service,
                                 @Value("${app.k8s.secret-name:travelmate-secrets}") String secretName,
                                 @Value("${app.k8s.configmap-name:travelmate-config}") String configMapName) {
        this.service = service; this.secretName = secretName; this.configMapName = configMapName;
    }

    @GetMapping
    public Result<SecretConfig> get() {
        Map<String, String> secret = service.getSecret(secretName);
        Map<String, String> config = service.getConfigMap(configMapName);
        return Result.success(new SecretConfig(secret.get("deepseek-api-key"), secret.get("admin-register-secret"),
                Boolean.parseBoolean(config.getOrDefault("ADMIN_REGISTER_ENABLED", "false")),
                config.getOrDefault("ADMIN_REGISTER_EXPIRES_AT", "")));
    }

    @PutMapping("/deepseek")
    public Result<String> updateDeepseek(@RequestBody DeepseekRequest request) {
        if (request == null || !StringUtils.hasText(request.apiKey()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API Key 不能为空");
        service.patchSecret(secretName, Map.of("deepseek-api-key", request.apiKey().trim()));
        service.restartDeployment("ai-service");
        return Result.success("DeepSeek API Key 已更新，AI 服务正在滚动重启");
    }

    @PutMapping("/admin-register")
    public Result<String> updateRegister(@RequestBody RegisterRequest request) {
        String secret = StringUtils.hasText(request.secret()) ? request.secret().trim() : generateSecret();
        service.patchSecret(secretName, Map.of("admin-register-secret", secret));
        Map<String, String> config = new HashMap<>();
        config.put("ADMIN_REGISTER_ENABLED", String.valueOf(request.enabled()));
        config.put("ADMIN_REGISTER_EXPIRES_AT", request.expiresAt() == null ? "" : request.expiresAt());
        service.patchConfigMap(configMapName, config);
        service.restartDeployment("identity-service");
        return Result.success("管理员注册配置已更新，身份服务正在滚动重启");
    }

    @PostMapping("/admin-register/reset")
    public Result<String> resetRegister() {
        service.patchSecret(secretName, Map.of("admin-register-secret", generateSecret()));
        service.restartDeployment("identity-service");
        return Result.success("管理员注册密钥已重置，身份服务正在滚动重启");
    }

    private String generateSecret() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record DeepseekRequest(String apiKey) {}
    public record RegisterRequest(String secret, boolean enabled, String expiresAt) {}
    public record SecretConfig(String deepseekApiKey, String adminRegisterSecret,
                               boolean adminRegisterEnabled, String adminRegisterExpiresAt) {}
}
