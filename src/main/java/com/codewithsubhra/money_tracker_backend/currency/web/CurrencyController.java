package com.codewithsubhra.money_tracker_backend.currency.web;

import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.currency.CurrencyService;
import com.codewithsubhra.money_tracker_backend.currency.web.dto.CurrencyResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /** Supported currencies for the client's base-currency dropdown. */
    @GetMapping
    public ApiResponse<List<CurrencyResponse>> list() {
        return ApiResponse.ok(currencyService.supported());
    }
}
