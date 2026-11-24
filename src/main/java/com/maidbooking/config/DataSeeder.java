package com.maidbooking.config;

import com.maidbooking.application.maid.MaidService;
import com.maidbooking.application.maid.OnboardMaidCommand;
import com.maidbooking.domain.model.AvailabilityWindow;
import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** Loads a few demo maids so the API can be explored straight away. Disable with maidbooking.seed-data=false. */
@Component
@ConditionalOnProperty(name = "maidbooking.seed-data", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final Set<DayOfWeek> MON_TO_FRI = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
    private static final Set<DayOfWeek> MON_TO_SAT = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY);
    private static final Set<DayOfWeek> EVERY_DAY = Set.of(DayOfWeek.values());

    private final MaidService maidService;

    public DataSeeder(MaidService maidService) {
        this.maidService = maidService;
    }

    @Override
    public void run(String... args) {
        seed("Lakshmi", Gender.FEMALE, "Koramangala", Set.of(ServiceType.CLEANING, ServiceType.DISHWASHING),
                250, 4.7, windows(MON_TO_SAT, 8, 18));
        seed("Meena", Gender.FEMALE, "Koramangala", Set.of(ServiceType.COOKING, ServiceType.CLEANING),
                350, 4.5, windows(EVERY_DAY, 7, 13));
        seed("Ravi", Gender.MALE, "Indiranagar", Set.of(ServiceType.CLEANING, ServiceType.LAUNDRY),
                220, 4.1, windows(MON_TO_FRI, 9, 17));
        seed("Sunita", Gender.FEMALE, "HSR Layout",
                Set.of(ServiceType.COOKING, ServiceType.DISHWASHING, ServiceType.LAUNDRY),
                300, 4.9, windows(MON_TO_SAT, 6, 20));
    }

    private void seed(String name, Gender gender, String locality, Set<ServiceType> services, long hourlyRate,
                      double rating, List<AvailabilityWindow> availability) {
        Maid maid = maidService.onboard(new OnboardMaidCommand(
                name, gender, locality, services, Money.of(hourlyRate), rating, availability));
        log.info("Seeded maid {} ({}) with id {}", maid.name(), maid.locality(), maid.id());
    }

    private static List<AvailabilityWindow> windows(Set<DayOfWeek> days, int fromHour, int toHour) {
        return days.stream()
                .map(day -> new AvailabilityWindow(day, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0)))
                .toList();
    }
}
