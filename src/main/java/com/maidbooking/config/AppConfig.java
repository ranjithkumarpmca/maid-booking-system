package com.maidbooking.config;

import com.maidbooking.domain.pricing.HourlyRatePricing;
import com.maidbooking.domain.pricing.PricingStrategy;
import com.maidbooking.domain.pricing.SurchargePricing;
import com.maidbooking.domain.refund.RefundPolicy;
import com.maidbooking.domain.refund.TimeBasedRefundPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;

/**
 * Wires the framework-free domain policies. To change pricing or refund rules, change (or replace) a bean here.
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock(@Value("${maidbooking.time-zone:Asia/Kolkata}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }

    /** Base hourly price, with a weekend surcharge on top (a simple dynamic-pricing rule). */
    @Bean
    public PricingStrategy pricingStrategy(
            @Value("${maidbooking.pricing.weekend-multiplier:1.20}") BigDecimal weekendMultiplier) {
        return SurchargePricing.weekend(new HourlyRatePricing(), weekendMultiplier);
    }

    @Bean
    public RefundPolicy refundPolicy() {
        return TimeBasedRefundPolicy.standard();
    }
}
