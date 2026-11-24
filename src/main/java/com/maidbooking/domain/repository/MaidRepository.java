package com.maidbooking.domain.repository;

import com.maidbooking.domain.model.Maid;

import java.util.List;
import java.util.Optional;

public interface MaidRepository {

    Maid save(Maid maid);

    Optional<Maid> findById(String id);

    List<Maid> findAll();
}
