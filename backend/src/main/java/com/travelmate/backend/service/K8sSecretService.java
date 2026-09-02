package com.travelmate.backend.service;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class K8sSecretService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(K8sSecretService.class);

    private static final String K8S_NAMESPACE_FILE = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

    private final KubernetesClient client;
    private final String namespace;
    private final boolean inCluster;

    private final Map<String, String> localSecrets = new ConcurrentHashMap<>();
    private final Map<String, String> localConfig = new ConcurrentHashMap<>();

    public K8sSecretService(
            @Value("${app.security.admin-register-secret:}") String adminRegisterSecret,
            @Value("${ai.deepseek.api-key:}") String deepseekApiKey,
            @Value("${ADMIN_REGISTER_ENABLED:false}") String adminRegisterEnabled,
            @Value("${ADMIN_REGISTER_EXPIRES_AT:}") String adminRegisterExpiresAt) {

        this.localSecrets.put("admin-register-secret", adminRegisterSecret);
        this.localSecrets.put("deepseek-api-key", deepseekApiKey);
        this.localConfig.put("ADMIN_REGISTER_ENABLED", adminRegisterEnabled);
        this.localConfig.put("ADMIN_REGISTER_EXPIRES_AT", adminRegisterExpiresAt);

        KubernetesClient candidate = null;
        String ns = null;
        boolean cluster = false;
        try {
            if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
                candidate = new KubernetesClientBuilder().build();
                ns = readNamespace();
                cluster = true;
                log.info("Kubernetes client initialized in namespace {}", ns);
            } else {
                log.info("Not running inside Kubernetes; secret/config management will use local fallback");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Kubernetes client, falling back to local environment", e);
        }
        this.client = candidate;
        this.namespace = ns != null ? ns : "default";
        this.inCluster = cluster;
    }

    private String readNamespace() throws IOException {
        Path path = Paths.get(K8S_NAMESPACE_FILE);
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        }
        return "default";
    }

    public boolean isInCluster() {
        return inCluster;
    }

    public Map<String, String> getSecret(String name) {
        if (!inCluster) {
            return new HashMap<>(localSecrets);
        }
        Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
        if (secret == null || secret.getData() == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        secret.getData().forEach((key, value) ->
                result.put(key, new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)));
        return result;
    }

    public void patchSecret(String name, Map<String, String> updates) {
        if (!inCluster) {
            localSecrets.putAll(updates);
            log.info("Local secret [{}] updated (Kubernetes unavailable)", name);
            return;
        }
        client.secrets().inNamespace(namespace).withName(name).edit(secret -> {
            Map<String, String> data = new HashMap<>();
            if (secret.getData() != null) {
                data.putAll(secret.getData());
            }
            updates.forEach((key, value) ->
                    data.put(key, Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))));
            return new SecretBuilder(secret)
                    .withData(data)
                    .build();
        });
        log.info("Secret [{}] updated in namespace {}", name, namespace);
    }

    public Map<String, String> getConfigMap(String name) {
        if (!inCluster) {
            return new HashMap<>(localConfig);
        }
        ConfigMap configMap = client.configMaps().inNamespace(namespace).withName(name).get();
        if (configMap == null || configMap.getData() == null) {
            return Map.of();
        }
        return new HashMap<>(configMap.getData());
    }

    public void patchConfigMap(String name, Map<String, String> updates) {
        if (!inCluster) {
            localConfig.putAll(updates);
            log.info("Local configmap [{}] updated (Kubernetes unavailable)", name);
            return;
        }
        client.configMaps().inNamespace(namespace).withName(name).edit(configMap -> {
            Map<String, String> data = new HashMap<>();
            if (configMap.getData() != null) {
                data.putAll(configMap.getData());
            }
            data.putAll(updates);
            return new ConfigMapBuilder(configMap)
                    .withData(data)
                    .build();
        });
        log.info("ConfigMap [{}] updated in namespace {}", name, namespace);
    }

    public void restartDeployment(String name) {
        if (!inCluster) {
            log.info("Deployment restart [{}] skipped (Kubernetes unavailable)", name);
            return;
        }
        client.apps().deployments().inNamespace(namespace).withName(name).rolling().restart();
        log.info("Deployment [{}] rolling restart triggered in namespace {}", name, namespace);
    }

    @PreDestroy
    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
