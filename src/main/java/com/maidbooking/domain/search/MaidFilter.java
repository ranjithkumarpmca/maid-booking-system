package com.maidbooking.domain.search;

import com.maidbooking.domain.model.Maid;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * One discovery filter. Each filter reads its own parameter(s) from the raw query parameters and contributes a
 * predicate only when its parameter is present. The search service never knows which filters exist, so adding
 * a filter is a new class and nothing else.
 */
public interface MaidFilter {

    Optional<Predicate<Maid>> predicateFor(Map<String, String> params);
}
