package com.maidbooking.domain.pricing;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;

/** Decides what one slot of a maid's time costs. Strategies can be stacked as decorators. */
public interface PricingStrategy {

    Money priceFor(Maid maid, ServiceType serviceType, TimeSlot slot);
}
