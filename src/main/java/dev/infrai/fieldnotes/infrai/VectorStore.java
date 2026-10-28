package dev.infrai.fieldnotes.infrai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.infrai.fieldnotes.config.InfraiProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class VectorStore {
    private final RestClient http;
    private final ObjectMapper json;
    private final InfraiProperties properties;

    public VectorStore(RestClient infraiRestClient, ObjectMapper json, InfraiProperties properties) {
        this.http = infraiRestClient;
        this.json = json;
        this.properties = properties;
    }

    public void createCollection(int dimension) {
        call("/vector/collection/create", Map.of(
                "collection", properties.collection(),
                "dimension", dimension,
                "metric", "cosine",
                "metadata", Map.of("purpose", "field-service voice notes")
        ), "collection-" + properties.collection());
    }

    public void upsert(String noteId, List<Float> embedding, Map<String, Object> metadata) {
        call("/vector/upsert", Map.of(
                "collection", properties.collection(),
                "vectors", List.of(Map.of(
                        "id", noteId,
                        "embedding", embedding,
                        "metadata", metadata
                ))
        ), "voice-note-" + noteId);
    }

    public JsonNode query(List<Float> embedding, Map<String, Object> filter) {
        return call("/vector/query", Map.of(
                "collection", properties.collection(),
                "embedding", embedding,
                "top_k", properties.topK(),
                "filter", filter,
                "include_metadata", true
        ), null).path("items");
    }

    private JsonNode call(String path, Map<String, Object> body, String idempotencyKey) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                RestClient.RequestBodySpec request = http.method(HttpMethod.POST)
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON);
                if (idempotencyKey != null) {
                    request.header("Idempotency-Key", idempotencyKey);
                }
                var response = request.body(body).retrieve().toEntity(String.class);
                return decode(response.getBody(), response.getStatusCode().value());
            } catch (RestClientResponseException response) {
                JsonNode envelope = parse(response.getResponseBodyAsString());
                if (response.getStatusCode().value() == 429 && attempt < 3) {
                    pause(retryDelay(response.getResponseHeaders(), attempt));
                    continue;
                }
                return unwrap(envelope, response.getStatusCode().value());
            }
        }
        throw new IllegalStateException("Retry loop exhausted");
    }

    private JsonNode decode(String raw, int status) {
        return unwrap(parse(raw), status);
    }

    private JsonNode parse(String raw) {
        try {
            return json.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Infrai returned a non-JSON response", e);
        }
    }

    private static JsonNode unwrap(JsonNode envelope, int status) {
        if (!envelope.path("ok").asBoolean(false)) {
            JsonNode error = envelope.path("error");
            throw new InfraiException(error.path("code").asText("REQUEST_REJECTED"),
                    error.path("message").asText("Request rejected"), status);
        }
        return envelope.path("data");
    }

    private static Duration retryDelay(HttpHeaders headers, int attempt) {
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value != null && value.matches("\\d+")) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        return Duration.ofMillis(250L * (1L << attempt));
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry", e);
        }
    }
}
