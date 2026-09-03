package com.travelmate;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class K8sResourceSerializationTests {

    private static final String MANAGED_SECRET = """
            {
              "apiVersion": "v1",
              "kind": "Secret",
              "metadata": {
                "name": "travelmate-secrets",
                "managedFields": [{
                  "apiVersion": "v1",
                  "fieldsType": "FieldsV1",
                  "fieldsV1": {"f:data": {"f:deepseek-api-key": {}}},
                  "manager": "kubectl",
                  "operation": "Update"
                }]
              },
              "data": {"deepseek-api-key": "b2xkLWtleQ=="}
            }
            """;

    private static final String MANAGED_CONFIG_MAP = """
            {
              "apiVersion": "v1",
              "kind": "ConfigMap",
              "metadata": {
                "name": "travelmate-config",
                "managedFields": [{
                  "apiVersion": "v1",
                  "fieldsType": "FieldsV1",
                  "fieldsV1": {"f:data": {"f:ADMIN_REGISTER_ENABLED": {}}},
                  "manager": "kubectl",
                  "operation": "Update"
                }]
              },
              "data": {"ADMIN_REGISTER_ENABLED": "false"}
            }
            """;

    private final KubernetesSerialization serialization = new KubernetesSerialization();

    @Test
    void serializesEditedSecretReturnedByKubernetes() {
        Secret current = serialization.unmarshal(MANAGED_SECRET, Secret.class);
        Secret updated = new SecretBuilder(current)
                .addToData(Map.of("deepseek-api-key", "bmV3LWtleQ=="))
                .build();

        assertThatCode(() -> serialization.asJson(updated)).doesNotThrowAnyException();
    }

    @Test
    void serializesEditedConfigMapReturnedByKubernetes() {
        ConfigMap current = serialization.unmarshal(MANAGED_CONFIG_MAP, ConfigMap.class);
        ConfigMap updated = new ConfigMapBuilder(current)
                .addToData(Map.of("ADMIN_REGISTER_ENABLED", "true"))
                .build();

        assertThatCode(() -> serialization.asJson(updated)).doesNotThrowAnyException();
    }
}
