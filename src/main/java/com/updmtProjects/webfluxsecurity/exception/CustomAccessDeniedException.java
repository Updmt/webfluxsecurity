package com.updmtProjects.webfluxsecurity.exception;

public class CustomAccessDeniedException extends RuntimeException {

    public CustomAccessDeniedException(String message) {
        super(message);
    }
}
