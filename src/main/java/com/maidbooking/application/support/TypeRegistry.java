package com.maidbooking.application.support;

import com.maidbooking.domain.exception.InvalidRequestException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Looks up the implementation registered for a type key (booking type, payment method...).
 * Implementations are collected by the DI container, so adding one never touches the code that uses the registry.
 */
public final class TypeRegistry<K, V> {

    private final String kind;
    private final Map<K, V> entries = new HashMap<>();

    public TypeRegistry(String kind, Collection<? extends V> implementations, Function<? super V, K> keyOf) {
        this.kind = kind;
        for (V implementation : implementations) {
            K key = keyOf.apply(implementation);
            if (entries.putIfAbsent(key, implementation) != null) {
                throw new IllegalStateException("Duplicate " + kind + " implementation for " + key);
            }
        }
    }

    public V get(K key) {
        V implementation = entries.get(key);
        if (implementation == null) {
            throw new InvalidRequestException("Unsupported " + kind + ": " + key);
        }
        return implementation;
    }
}
