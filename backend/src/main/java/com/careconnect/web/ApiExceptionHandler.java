package com.careconnect.web;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Every error leaves the API as {"message": "..."} so the UI can show it directly. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static ResponseEntity<Map<String, String>> body(HttpStatusCode s, String m) { return ResponseEntity.status(s).body(Map.of("message", m)); }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return body(e.getStatusCode(), e.getReason() == null ? "Request failed" : e.getReason());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        var f = e.getBindingResult().getFieldError();
        return body(HttpStatus.BAD_REQUEST, f == null ? "Invalid request" : f.getField() + ": " + f.getDefaultMessage());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) { return body(HttpStatus.BAD_REQUEST, "Malformed request body"); }
    @ExceptionHandler(DisabledException.class)
    ResponseEntity<Map<String, String>> disabled(DisabledException e) { return body(HttpStatus.FORBIDDEN, "This account has been disabled"); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, String>> auth(AuthenticationException e) { return body(HttpStatus.UNAUTHORIZED, "Invalid email or password"); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> denied(AccessDeniedException e) { return body(HttpStatus.FORBIDDEN, "You do not have access to this"); }
}
