package com.codewithsubhra.money_tracker_backend.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " was not found");
    }
}
