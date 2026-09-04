package com.travelmate.microservices.ops;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpsK8sSecretService implements AutoCloseable {
    private static final Path NAMESPACE_FILE = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
    private final KubernetesClient client;
    private final String namespace;
    private final Map<String, String> localSecrets = new ConcurrentHashMap<>();
    private final Map<String, String> localConfig = new ConcurrentHashMap<>();

    public OpsK8sSecretService(@Value("${app.security.admin-register-secret:}") String registerSecret,
                               @Value("${ai.deepseek.api-key:}") String deepseekKey,
                               @Value("${ADMIN_REGISTER_ENABLED:false}") String registerEnabled,
                               @Value("${ADMIN_REGISTER_EXPIRES_AT:}") String registerExpiresAt) {
        localSecrets.put("admin-register-secret", registerSecret);
        localSecrets.put("deepseek-api-key", deepseekKey);
        localConfig.put("ADMIN_REGISTER_ENABLED", registerEnabled);
        localConfig.put("ADMIN_REGISTER_EXPIRES_AT", registerExpiresAt);
        KubernetesClient candidate = null;
        String resolvedNamespace = "default";
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
            candidate = new KubernetesClientBuilder().build();
            try {
                if (Files.exists(NAMESPACE_FILE)) resolvedNamespace = Files.readString(NAMESPACE_FILE).trim();
            } catch (Exception exception) {
                candidate.close();
                throw new IllegalStateException("无法读取 Kubernetes 命名空间", exception);
            }
        }
        client = candidate;
        namespace = resolvedNamespace;
    }

    public Map<String, String> getSecret(String name) {
        if (client == null) return new HashMap<>(localSecrets);
        var resource = client.secrets().inNamespace(namespace).withName(name).get();
        if (resource == null || resource.getData() == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        resource.getData().forEach((key, value) -> result.put(key,
                new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)));
        return result;
    }

    public Map<String, String> getConfigMap(String name) {
        if (client == null) return new HashMap<>(localConfig);
        var resource = client.configMaps().inNamespace(namespace).withName(name).get();
        return resource == null || resource.getData() == null ? Map.of() : new HashMap<>(resource.getData());
    }

    public void patchSecret(String name, Map<String, String> updates) {
        if (client == null) { localSecrets.putAll(updates); return; }
        client.secrets().inNamespace(namespace).withName(name).edit(secret -> {
            Map<String, String> data = new HashMap<>(secret.getData() == null ? Map.of() : secret.getData());
            updates.forEach((key, value) -> data.put(key,
                    Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))));
            return new SecretBuilder(secret).withData(data).build();
        });
    }

    public void patchConfigMap(String name, Map<String, String> updates) {
        if (client == null) { localConfig.putAll(updates); return; }
        client.configMaps().inNamespace(namespace).withName(name).edit(config -> {
            Map<String, String> data = new HashMap<>(config.getData() == null ? Map.of() : config.getData());
            data.putAll(updates);
            return new ConfigMapBuilder(config).withData(data).build();
        });
    }

    public void restartDeployment(String name) {
        if (client != null) client.apps().deployments().inNamespace(namespace).withName(name).rolling().restart();
    }

    @PreDestroy @Override public void close() { if (client != null) client.close(); }
}
