package dev.infrai.healthupload.infrai;

import com.fasterxml.jackson.databind.JsonNode;

public final class InfraiApiException extends RuntimeException {
    private final int status;
    private final String code;
    private final JsonNode details;

    public InfraiApiException(int status, String code, JsonNode details) {
        super(details.path("message").asText(code));
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public int status() { return status; }
    public String code() { return code; }
    public JsonNode details() { return details; }
}
