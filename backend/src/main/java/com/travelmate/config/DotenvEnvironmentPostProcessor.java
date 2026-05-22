package com.travelmate.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "travelmateDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (Path path : getCandidatePaths()) {
            loadDotenv(path, values);
        }

        if (values.isEmpty()) {
            return;
        }

        if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addAfter(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(PROPERTY_SOURCE_NAME, values)
            );
        } else {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }

    private List<Path> getCandidatePaths() {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        return List.of(
                workingDirectory.resolve(".env"),
                workingDirectory.resolve("..").resolve(".env").normalize()
        );
    }

    private void loadDotenv(Path path, Map<String, Object> values) {
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line, values);
            }
        } catch (IOException ignored) {
            // .env is optional; unreadable files should not block startup.
        }
    }

    private void parseLine(String line, Map<String, Object> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = trimmed.substring(0, separatorIndex).trim();
        if (!key.matches("[A-Z_][A-Z0-9_]*") || values.containsKey(key)) {
            return;
        }

        String value = trimmed.substring(separatorIndex + 1).trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        values.put(key, value);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
