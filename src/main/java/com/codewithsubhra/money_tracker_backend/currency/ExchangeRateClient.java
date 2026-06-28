package com.codewithsubhra.money_tracker_backend.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client over the frankfurter.app FX API with simple in-memory caching.
 * Rates change at most once per working day, so a multi-hour TTL keeps us well
 * within any fair-use limits while staying fresh enough for a personal app.
 */
@Component
public class ExchangeRateClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateClient.class);

    private final RestClient http;
    private final Duration ttl;

    // key "FROM->TO" -> cached rate; plus a single cached currency catalogue.
    private final Map<String, Cached<BigDecimal>> rateCache = new ConcurrentHashMap<>();
    private volatile Cached<Map<String, String>> currencyCache;

    public ExchangeRateClient(RestClient fxRestClient, FxProperties properties) {
        this.http = fxRestClient;
        this.ttl = Duration.ofMinutes(properties.cacheTtlMinutes());
    }

    /** Exchange rate to multiply a {@code from} amount by to get {@code to}. */
    public BigDecimal rate(String from, String to) {
        String f = from.trim().toUpperCase();
        String t = to.trim().toUpperCase();
        if (f.equals(t)) {
            return BigDecimal.ONE;
        }
        String key = f + "->" + t;
        Cached<BigDecimal> hit = rateCache.get(key);
        if (hit != null && hit.fresh(ttl)) {
            return hit.value();
        }
        BigDecimal fetched = fetchRate(f, t);
        rateCache.put(key, new Cached<>(fetched, Instant.now()));
        return fetched;
    }

    /** Supported currency codes mapped to their display names (e.g. USD -> "US Dollar"). */
    public Map<String, String> currencies() {
        Cached<Map<String, String>> hit = currencyCache;
        if (hit != null && hit.fresh(ttl)) {
            return hit.value();
        }
        Map<String, String> fetched = fetchCurrencies();
        currencyCache = new Cached<>(fetched, Instant.now());
        return fetched;
    }

    private BigDecimal fetchRate(String from, String to) {
        try {
            LatestResponse body = http.get()
                    .uri(uri -> uri.path("/latest").queryParam("from", from).queryParam("to", to).build())
                    .retrieve()
                    .body(LatestResponse.class);
            BigDecimal rate = body == null || body.rates() == null ? null : body.rates().get(to);
            if (rate == null) {
                throw new FxUnavailableException(
                        "No exchange rate available for %s -> %s".formatted(from, to));
            }
            return rate;
        } catch (RestClientException ex) {
            log.warn("FX rate lookup {} -> {} failed: {}", from, to, ex.getMessage());
            throw new FxUnavailableException("Could not reach the exchange-rate service");
        }
    }

    private Map<String, String> fetchCurrencies() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> body = http.get().uri("/currencies").retrieve().body(Map.class);
            if (body == null || body.isEmpty()) {
                throw new FxUnavailableException("Could not load the supported currency list");
            }
            return body;
        } catch (RestClientException ex) {
            log.warn("FX currency catalogue lookup failed: {}", ex.getMessage());
            throw new FxUnavailableException("Could not reach the exchange-rate service");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatestResponse(String base, Map<String, BigDecimal> rates) {
    }

    private record Cached<T>(T value, Instant fetchedAt) {
        boolean fresh(Duration ttl) {
            return fetchedAt.plus(ttl).isAfter(Instant.now());
        }
    }
}
