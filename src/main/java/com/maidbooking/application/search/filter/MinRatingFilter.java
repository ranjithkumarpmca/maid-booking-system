package com.maidbooking.application.search.filter;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** {@code minRating=4.0}. */
@Component
public class MinRatingFilter implements MaidFilter {

    @Override
    public Optional<Predicate<Maid>> predicateFor(Map<String, String> params) {
        return FilterParams.text(params, "minRating")
                .map(raw -> FilterParams.number("minRating", raw))
                .map(min -> maid -> maid.rating() >= min);
    }
}
