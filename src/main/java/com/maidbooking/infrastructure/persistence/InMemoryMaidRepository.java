package com.maidbooking.infrastructure.persistence;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.repository.MaidRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMaidRepository implements MaidRepository {

    private final Map<String, Maid> store = new ConcurrentHashMap<>();

    @Override
    public Maid save(Maid maid) {
        store.put(maid.id(), maid);
        return maid;
    }

    @Override
    public Optional<Maid> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Maid> findAll() {
        return List.copyOf(store.values());
    }
}
