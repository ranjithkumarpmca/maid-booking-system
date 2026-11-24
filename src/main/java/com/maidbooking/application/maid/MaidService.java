package com.maidbooking.application.maid;

import com.maidbooking.domain.exception.ResourceNotFoundException;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.repository.MaidRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MaidService {

    private final MaidRepository maids;

    public MaidService(MaidRepository maids) {
        this.maids = maids;
    }

    /** Profile validation lives in the {@link Maid} constructor, so the rules hold however a maid is created. */
    public Maid onboard(OnboardMaidCommand command) {
        Maid maid = new Maid(
                UUID.randomUUID().toString(),
                command.name(),
                command.gender(),
                command.locality(),
                command.services(),
                command.hourlyRate(),
                command.rating() == null ? 0.0 : command.rating(),
                command.availability());
        return maids.save(maid);
    }

    public Maid get(String maidId) {
        return maids.findById(maidId).orElseThrow(() -> new ResourceNotFoundException("Maid", maidId));
    }
}
