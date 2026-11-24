package com.maidbooking.application.maid;

import com.maidbooking.domain.model.AvailabilityWindow;
import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;

import java.util.List;
import java.util.Set;

/** Input for onboarding a maid. {@code rating} is optional; new maids start unrated (0). */
public record OnboardMaidCommand(String name, Gender gender, String locality, Set<ServiceType> services,
                                 Money hourlyRate, Double rating, List<AvailabilityWindow> availability) {
}
