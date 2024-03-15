package com.updmtProjects.webfluxsecurity.exception.handler;

import com.updmtProjects.webfluxsecurity.exception.AuthException;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomNotFoundException.class)
    public Mono<ResponseEntity<String>> handleCustomNotFoundException(CustomNotFoundException ex) {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ex.getMessage())
        );
    }

    @ExceptionHandler(CustomAccessDeniedException.class)
    public Mono<ResponseEntity<String>> handleCustomNotFoundException(CustomAccessDeniedException ex) {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ex.getMessage())
        );
    }

    @ExceptionHandler(AuthException.class)
    public Mono<ResponseEntity<String>> handleCustomNotFoundException(AuthException ex) {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ex.getMessage())
        );
    }
}
