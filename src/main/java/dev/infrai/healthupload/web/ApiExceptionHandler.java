package dev.infrai.healthupload.web;

import com.openai.errors.OpenAIServiceException;
import dev.infrai.healthupload.infrai.InfraiApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseEntity<Map<String, Object>> moderationRejection(OpenAIServiceException exception) {
        HttpStatus status = exception.statusCode() >= 400 && exception.statusCode() < 500
                ? HttpStatus.valueOf(exception.statusCode())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "code", exception.code().orElse("MODERATION_REJECTED"),
                "message", "The moderation request was rejected"));
    }

    @ExceptionHandler(InfraiApiException.class)
    public ResponseEntity<Map<String, Object>> infraiRejection(InfraiApiException exception) {
        HttpStatus status = exception.status() >= 400 && exception.status() < 500
                ? HttpStatus.valueOf(exception.status())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "code", exception.code(),
                "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "code", "INVALID_SUBMISSION",
                "message", exception.getMessage()));
    }
}
