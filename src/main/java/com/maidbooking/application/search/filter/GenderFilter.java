package com.maidbooking.application.search.filter;

import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** {@code gender=FEMALE}: the customer's gender preference. */
@Component
public class GenderFilter implements MaidFilter {

    @Override
    public Optional<Predicate<Maid>> predicateFor(Map<String, String> params) {
        return FilterParams.text(params, "gender")
                .map(raw -> FilterParams.enumValue(Gender.class, "gender", raw))
                .map(gender -> maid -> maid.gender() == gender);
    }
}
