package com.maidbooking.domain.pricing;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;

/** Base price: the maid's hourly rate multiplied by the slot length. */
public class HourlyRatePricing implements PricingStrategy {

    @Override
    public Money priceFor(Maid maid, ServiceType serviceType, TimeSlot slot) {
        return maid.hourlyRate().times(slot.hours());
    }
}
