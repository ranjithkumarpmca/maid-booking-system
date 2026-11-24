package com.maidbooking.application.search.filter;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** {@code service=COOKING}: the maid offers that service. */
@Component
public class ServiceTypeFilter implements MaidFilter {

    @Override
    public Optional<Predicate<Maid>> predicateFor(Map<String, String> params) {
        return FilterParams.text(params, "service")
                .map(raw -> FilterParams.enumValue(ServiceType.class, "service", raw))
                .map(service -> maid -> maid.offers(service));
    }
}
