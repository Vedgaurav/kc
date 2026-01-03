package com.one.kc.common.exceptions;

import org.springframework.http.HttpStatus;

public class UserFacingException extends RuntimeException {

    private final HttpStatus status;

    public UserFacingException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public UserFacingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
