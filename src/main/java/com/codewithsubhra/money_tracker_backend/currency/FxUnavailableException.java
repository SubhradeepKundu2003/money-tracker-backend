package com.codewithsubhra.money_tracker_backend.currency;

import com.codewithsubhra.money_tracker_backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Raised when the upstream exchange-rate provider can't be reached or returns no rate. */
public class FxUnavailableException extends ApiException {

    public FxUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "FX_UNAVAILABLE", message);
    }
}
