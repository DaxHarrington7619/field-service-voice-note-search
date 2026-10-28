package dev.infrai.fieldnotes.web;

import dev.infrai.fieldnotes.infrai.InfraiException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(InfraiException.class)
    ResponseEntity<Map<String, Object>> rejected(InfraiException error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(Map.of(
                "code", error.code(),
                "message", error.getMessage()
        ));
    }
}
