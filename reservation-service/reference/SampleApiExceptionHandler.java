package com.breezewood.reservation.reference;

// REFERENCE EXAMPLE ONLY -- mirrors the real ApiExceptionHandler's pattern:
// translate a business-rule rejection into a proper HTTP error response
// instead of letting it surface as an unhandled 500.
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = SampleStockController.class)
public class SampleApiExceptionHandler {

    @ExceptionHandler(SampleOutOfStockException.class)
    public ResponseEntity<Map<String, String>> handleOutOfStock(SampleOutOfStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
