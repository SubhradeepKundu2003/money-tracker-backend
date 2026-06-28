package com.codewithsubhra.money_tracker_backend.currency;

import com.codewithsubhra.money_tracker_backend.common.exception.BadRequestException;
import com.codewithsubhra.money_tracker_backend.currency.web.dto.CurrencyResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Currency catalogue + conversion helpers backed by {@link ExchangeRateClient}. */
@Service
public class CurrencyService {

    /** Money is stored with 2 decimal places throughout the domain. */
    private static final int MONEY_SCALE = 2;

    private final ExchangeRateClient rates;

    public CurrencyService(ExchangeRateClient rates) {
        this.rates = rates;
    }

    /** Supported currencies, sorted by code, for the client's dropdown. */
    public List<CurrencyResponse> supported() {
        return rates.currencies().entrySet().stream()
                .map(e -> new CurrencyResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CurrencyResponse::code))
                .toList();
    }

    /** Normalizes and validates a currency code against the supported set. */
    public String requireSupported(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Currency is required");
        }
        String normalized = code.trim().toUpperCase();
        Map<String, String> catalogue = rates.currencies();
        if (!catalogue.containsKey(normalized)) {
            throw new BadRequestException("Unsupported currency: " + normalized);
        }
        return normalized;
    }

    /** Converts {@code amount} from one currency to another at the current rate, rounded to 2dp. */
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) {
            return null;
        }
        BigDecimal rate = rates.rate(from, to);
        return amount.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
