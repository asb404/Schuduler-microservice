package com.schuduler.programschuduler.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebInputException;

// import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Order(0)
public class GlobalErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleBadArg(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        var m = new HashMap<String,String>();
        m.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(m);
    }

    // @ExceptionHandler(ConstraintViolationException.class)
    // public ResponseEntity<Map<String,String>> handleValidation(ConstraintViolationException ex) {
    //     log.warn("Validation failed: {}", ex.getMessage());
    //     var m = new HashMap<String,String>();
    //     m.put("error", ex.getMessage());
    //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(m);
    // }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Map<String,String>> handleBind(ServerWebInputException ex) {
        log.warn("Bind error: {}", ex.getReason(), ex);
        var m = new HashMap<String,String>();
        m.put("error", ex.getReason() == null ? ex.getMessage() : ex.getReason());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(m);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> handleAll(Exception ex) {
        // Log full stack trace to server logs for debugging
        log.error("Unhandled exception: ", ex);
        var m = new HashMap<String,String>();
        m.put("error", "Internal server error");
        m.put("message", ex.getMessage() == null ? "unknown" : ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(m);
    }
}
