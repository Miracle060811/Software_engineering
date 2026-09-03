package com.travelmate.controller;

import com.travelmate.backend.service.K8sSecretService;
import com.travelmate.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/secrets")
public class AdminSecretController {

    private static final Logger log = LoggerFactory.getLogger(AdminSecretController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private K8sSecretService k8sSecretService;

    @Value("${app.k8s.secret-name:travelmate-secrets}")
    private String secretName;

    @Value("${app.k8s.configmap-name:travelmate-config}")
    private String configMapName;

    @Value("${app.k8s.deployment-name:travelmate-backend}")
    private String deploymentName;

    @GetMapping
    public Result<SecretConfigVo> getSecretConfig() {
        Map<String, String> secret = k8sSecretService.getSecret(secretName);
        Map<String, String> config = k8sSecretService.getConfigMap(configMapName);
        return Result.success(new SecretConfigVo(
                secret.get("deepseek-api-key"),
                secret.get("admin-register-secret"),
                Boolean.parseBoolean(config.getOrDefault("ADMIN_REGISTER_ENABLED", "false")),
                config.getOrDefault("ADMIN_REGISTER_EXPIRES_AT", "")
        ));
    }

    @PutMapping("/deepseek")
    public Result<String> updateDeepseekApiKey(@RequestBody DeepseekKeyRequest request) {
        if (!StringUtils.hasText(request.apiKey())) {
            return Result.error("API Key 不能为空");
        }
        k8sSecretService.patchSecret(secretName, Map.of("deepseek-api-key", request.apiKey()));
        k8sSecretService.restartDeployment(deploymentName);
        log.info("DeepSeek API Key updated by admin, deployment {} restarted", deploymentName);
        return Result.success("DeepSeek API Key 已更新，后端正在滚动重启以生效");
    }

    @PutMapping("/admin-register")
    public Result<String> updateAdminRegisterConfig(@RequestBody AdminRegisterConfigRequest request) {
        String secret = StringUtils.hasText(request.secret()) ? request.secret() : generateSecret();
        k8sSecretService.patchSecret(secretName, Map.of("admin-register-secret", secret));

        Map<String, String> configUpdates = new HashMap<>();
        configUpdates.put("ADMIN_REGISTER_ENABLED", String.valueOf(request.enabled()));
        if (request.expiresAt() != null) {
            configUpdates.put("ADMIN_REGISTER_EXPIRES_AT", request.expiresAt());
        }
        k8sSecretService.patchConfigMap(configMapName, configUpdates);
        k8sSecretService.restartDeployment(deploymentName);
        log.info("Admin register config updated by admin, deployment {} restarted", deploymentName);
        return Result.success("管理员注册配置已更新，后端正在滚动重启以生效");
    }

    @PostMapping("/admin-register/reset")
    public Result<String> resetAdminRegisterSecret() {
        String secret = generateSecret();
        k8sSecretService.patchSecret(secretName, Map.of("admin-register-secret", secret));
        k8sSecretService.restartDeployment(deploymentName);
        log.info("Admin register secret reset by admin, deployment {} restarted", deploymentName);
        return Result.success("管理员注册密钥已重置，后端正在滚动重启以生效");
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record DeepseekKeyRequest(String apiKey) {
    }

    public record AdminRegisterConfigRequest(String secret, boolean enabled, String expiresAt) {
    }

    public record SecretConfigVo(String deepseekApiKey,
                                  String adminRegisterSecret,
                                  boolean adminRegisterEnabled,
                                  String adminRegisterExpiresAt) {
    }
}
