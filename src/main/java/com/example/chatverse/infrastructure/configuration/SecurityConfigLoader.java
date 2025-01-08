package com.example.chatverse.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

@Component
public class SecurityConfigLoader {

    private final Map<String, String> config;

    public SecurityConfigLoader() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("security-config.json");
            if (inputStream == null) {
                throw new FileNotFoundException("security-config.json not found in classpath");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            this.config = objectMapper.readValue(inputStream, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load security configuration", e);
        }
    }

    public String getSecretKey() {
        return config.get("BASE64_SECRET_KEY");
    }
}
