package com.learnfast.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
    return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
  }
}
