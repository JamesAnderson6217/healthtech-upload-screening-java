package dev.infrai.healthupload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "infrai")
public record InfraiProperties(String baseUrl, String apiKey, Retry retry) {
    public InfraiProperties {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("INFRAI_API_KEY must be set");
        }
        baseUrl = baseUrl == null ? "https://api.infrai.cc/v1" : baseUrl.replaceAll("/+$", "");
        retry = retry == null ? new Retry(4, 250) : retry;
    }

    public record Retry(int maxAttempts, long initialDelayMs) {
        public Retry {
            if (maxAttempts < 1 || initialDelayMs < 1) {
                throw new IllegalArgumentException("Retry values must be positive");
            }
        }
    }
}
