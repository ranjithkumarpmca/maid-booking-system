package com.maidbooking.application.search.filter;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** {@code skills=COOKING,LAUNDRY}: the maid offers <em>all</em> of the listed skills. */
@Component
public class SkillsFilter implements MaidFilter {

    @Override
    public Optional<Predicate<Maid>> predicateFor(Map<String, String> params) {
        return FilterParams.text(params, "skills").map(raw -> {
            Set<ServiceType> required = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> FilterParams.enumValue(ServiceType.class, "skills", token))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ServiceType.class)));
            return maid -> maid.services().containsAll(required);
        });
    }
}
