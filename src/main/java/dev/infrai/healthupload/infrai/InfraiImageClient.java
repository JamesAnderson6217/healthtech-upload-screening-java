package dev.infrai.healthupload.infrai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.infrai.healthupload.config.InfraiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public final class InfraiImageClient implements ImageOperations {
    private final HttpClient http;
    private final ObjectMapper json;
    private final InfraiProperties config;

    @Autowired
    public InfraiImageClient(ObjectMapper json, InfraiProperties config) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), json, config);
    }

    InfraiImageClient(HttpClient http, ObjectMapper json, InfraiProperties config) {
        this.http = http;
        this.json = json;
        this.config = config;
    }

    @Override
    public UploadedImage upload(byte[] file, String filename, String idempotencyKey) {
        String boundary = "infrai-" + UUID.randomUUID();
        byte[] body = multipart(boundary, file, filename);
        HttpRequest request = request("/image/upload", idempotencyKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .method("POST", HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        JsonNode data = send(request);
        return new UploadedImage(requiredText(data, "id"), requiredText(data, "url"));
    }

    @Override
    public PublishedImage resize(String image, String idempotencyKey) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("image", image);
        fields.put("width", 1280);
        fields.put("height", 1280);
        fields.put("fit", "inside");
        fields.put("enlarge", false);
        fields.put("format", "webp");
        fields.put("store", true);
        try {
            HttpRequest request = request("/image/resize", idempotencyKey)
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(fields)))
                    .build();
            JsonNode data = send(request);
            return new PublishedImage(requiredText(data, "id"), requiredText(data, "url"));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode resize request", exception);
        }
    }

    private HttpRequest.Builder request(String path, String idempotencyKey) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Idempotency-Key", idempotencyKey);
    }

    private JsonNode send(HttpRequest request) {
        for (int attempt = 1; attempt <= config.retry().maxAttempts(); attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < config.retry().maxAttempts()) {
                    sleep(retryDelay(response, attempt));
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new InfraiApiException(response.statusCode(), error.path("code").asText("INFRAI_REJECTED"), error);
                }
                return envelope.path("data");
            } catch (InfraiApiException exception) {
                throw exception;
            } catch (IOException exception) {
                throw new IllegalStateException("Infrai response was not valid JSON", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", exception);
            }
        }
        throw new IllegalStateException("Retry loop ended unexpectedly");
    }

    private long retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> parseRetryAfter(value, attempt))
                .orElseGet(() -> config.retry().initialDelayMs() * (1L << (attempt - 1)));
    }

    private long parseRetryAfter(String value, int attempt) {
        try {
            return Math.max(1, Long.parseLong(value)) * 1_000L;
        } catch (NumberFormatException ignored) {
            return config.retry().initialDelayMs() * (1L << (attempt - 1));
        }
    }

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static String requiredText(JsonNode data, String field) {
        String value = data.path(field).asText();
        if (value.isBlank()) throw new IllegalStateException("Infrai response omitted " + field);
        return value;
    }

    private static byte[] multipart(String boundary, byte[] file, String filename) {
        String safeName = filename.replace("\"", "");
        String prefix = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String suffix = "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"filename\"\r\n\r\n"
                + safeName + "\r\n--" + boundary + "--\r\n";
        byte[] first = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] last = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[first.length + file.length + last.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(file, 0, result, first.length, file.length);
        System.arraycopy(last, 0, result, first.length + file.length, last.length);
        return result;
    }
}
