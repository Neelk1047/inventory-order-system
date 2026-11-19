package com.neel.inventoryordersystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice catches exceptions thrown ANYWHERE in the controllers/services
// and converts them into a consistent JSON error response.
// Rule of thumb used here:
//   4xx = the client did something wrong (we log at WARN or not at all)
//   5xx = our bug (we log the full stack trace at ERROR, but NEVER send details to the client)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFound(OrderNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 409 Conflict: the request is valid, but conflicts with the current state of the data
    @ExceptionHandler(ProductInUseException.class)
    public ResponseEntity<Map<String, String>> handleProductInUse(ProductInUseException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // Thrown when two requests modify the same row at the same time (optimistic locking, @Version).
    // The loser gets 409 and can simply retry.
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Map<String, String>> handleConcurrencyFailure(ConcurrencyFailureException ex) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        return buildResponse("The data was changed by another request. Please retry.", HttpStatus.CONFLICT);
    }

    // Last line of defence for database constraint violations (e.g. foreign keys)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return buildResponse("The operation conflicts with existing data.", HttpStatus.CONFLICT);
    }

    // Triggered automatically whenever @Valid validation fails on a request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Body is not valid JSON, or a field has the wrong type (e.g. "price": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse("Malformed or unreadable request body.", HttpStatus.BAD_REQUEST);
    }

    // e.g. GET /products/abc  (id should be a number)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse("Invalid value for parameter '" + ex.getName() + "'.", HttpStatus.BAD_REQUEST);
    }

    // Catch-all for anything unexpected.
    // Full details go to the log; the client only gets a generic message.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // Spring's own web errors (unknown URL -> 404, wrong HTTP method -> 405, ...) already carry
        // the right status code, so we keep it instead of turning everything into a 500.
        if (ex instanceof ErrorResponse errorResponse) {
            String detail = errorResponse.getBody().getDetail();
            return buildResponse(detail != null ? detail : "Request could not be processed.",
                    errorResponse.getStatusCode());
        }
        log.error("Unexpected error", ex);
        return buildResponse("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, String>> buildResponse(String message, HttpStatusCode status) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return new ResponseEntity<>(body, status);
    }
}
