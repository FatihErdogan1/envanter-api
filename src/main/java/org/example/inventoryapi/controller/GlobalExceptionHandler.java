package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.DeletionErrorResponse;
import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return error(404, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        log.warn("Yetkisiz erişim: {}", e.getMessage());
        return error(403, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException e) {
        log.warn("Yetkisiz erişim: {}", e.getMessage());
        return error(403, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(DeletionBlockedException.class)
    public ResponseEntity<DeletionErrorResponse> handleDeletionBlocked(DeletionBlockedException e) {
        return ResponseEntity.status(409)
                .body(DeletionErrorResponse.of(e.getMessage(), e.getCount(), e.getRelatedEntity()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("Veri bütünlüğü ihlali: {}", e.getMessage());
        return error(409, "CONFLICT", "Veri bütünlüğü ihlali: ilişkili kayıtlar mevcut.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return error(400, "BUSINESS_RULE_VIOLATION", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return error(400, "BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Beklenmeyen hata: {}", e.getMessage(), e);
        return error(500, "INTERNAL_SERVER_ERROR", "Beklenmeyen bir hata oluştu.");
    }

    private ResponseEntity<Map<String, Object>> error(int status, String errorType, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error",   errorType,
                "message", message,
                "status",  status
        ));
    }
}
