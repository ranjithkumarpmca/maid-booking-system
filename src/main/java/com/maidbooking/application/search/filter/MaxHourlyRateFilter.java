package com.maidbooking.application.search.filter;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** {@code maxPrice=300}: hourly rate at or below the given amount. */
@Component
public class MaxHourlyRateFilter implements MaidFilter {

    @Override
    public Optional<Predicate<Maid>> predicateFor(Map<String, String> params) {
        return FilterParams.text(params, "maxPrice")
                .map(raw -> FilterParams.decimal("maxPrice", raw))
                .map(max -> maid -> maid.hourlyRate().compareTo(Money.of(max)) <= 0);
    }
}
