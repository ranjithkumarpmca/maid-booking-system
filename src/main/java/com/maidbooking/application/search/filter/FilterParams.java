package com.maidbooking.application.search.filter;

import com.maidbooking.domain.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Small parsing helpers shared by filters, so each filter stays a few lines long. */
public final class FilterParams {

    private FilterParams() {
    }

    public static Optional<String> text(Map<String, String> params, String key) {
        return Optional.ofNullable(params.get(key)).map(String::trim).filter(value -> !value.isEmpty());
    }

    public static <E extends Enum<E>> E enumValue(Class<E> type, String key, String raw) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid value '" + raw + "' for '" + key + "'");
        }
    }

    public static double number(String key, String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("'" + key + "' must be a number, got '" + raw + "'");
        }
    }

    public static BigDecimal decimal(String key, String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("'" + key + "' must be a number, got '" + raw + "'");
        }
    }
}
