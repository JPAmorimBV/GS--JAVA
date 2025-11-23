package br.com.fiap.config;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

// Define explicit bean name to avoid conflict with br.com.fiap.exception.GlobalExceptionHandler
@Component("configGlobalExceptionHandler")
@ControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            // Resolve message via MessageSource to support i18n keys
            String errorMessage = messageSource.getMessage(error, LocaleContextHolder.getLocale());
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

@ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return new ResponseEntity<>(Collections.singletonMap("error", "Email already registered"), HttpStatus.CONFLICT);
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        String msg = messageSource.getMessage("auth.unauthorized", null, LocaleContextHolder.getLocale());
        return new ResponseEntity<>(Collections.singletonMap("error", msg), HttpStatus.FORBIDDEN);
    }

@ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        // Check if it's a DataIntegrityViolationException (duplicate key)
        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            return new ResponseEntity<>(Collections.singletonMap("error", "Email already registered"), HttpStatus.CONFLICT);
        }
        // Default error handling
        return new ResponseEntity<>(Collections.singletonMap("error", "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
    
