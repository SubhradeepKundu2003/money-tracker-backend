package com.codewithsubhra.money_tracker_backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base class for expected, client-facing errors carrying an HTTP status and code. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
