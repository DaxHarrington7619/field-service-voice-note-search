package dev.infrai.fieldnotes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("infrai")
public record InfraiProperties(String baseUrl, String apiKey, String collection, int topK) {
    public InfraiProperties {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("INFRAI_API_KEY is required");
        }
    }
}
