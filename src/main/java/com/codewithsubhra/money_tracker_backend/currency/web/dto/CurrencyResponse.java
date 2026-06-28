package com.codewithsubhra.money_tracker_backend.currency.web.dto;

/** One selectable currency for the client's dropdown. */
public record CurrencyResponse(String code, String name) {
}
