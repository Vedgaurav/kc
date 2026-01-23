package com.one.kc.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class UserFacingException extends RuntimeException {

    @Getter
    private final HttpStatus status;
    @Getter
    private final String errorCode;

    public UserFacingException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "00000";
    }

    public UserFacingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "00000";
    }

    public UserFacingException(String message,
                               String errorCode
    ) {
        super(message);
        this.errorCode = errorCode;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public UserFacingException(String message, HttpStatus status,
                               String errorCode
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

}
