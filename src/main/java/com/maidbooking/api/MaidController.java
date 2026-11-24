package com.maidbooking.api;

import com.maidbooking.api.dto.AddMaidRequest;
import com.maidbooking.api.dto.MaidResponse;
import com.maidbooking.api.dto.TimeSlotResponse;
import com.maidbooking.application.availability.AvailabilityService;
import com.maidbooking.application.maid.MaidService;
import com.maidbooking.application.search.MaidSearchQuery;
import com.maidbooking.application.search.MaidSearchService;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.TimeSlot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maids")
@Tag(name = "Maids", description = "Onboarding and discovery")
public class MaidController {

    private final MaidService maidService;
    private final MaidSearchService searchService;
    private final AvailabilityService availabilityService;

    public MaidController(MaidService maidService, MaidSearchService searchService,
                          AvailabilityService availabilityService) {
        this.maidService = maidService;
        this.searchService = searchService;
        this.availabilityService = availabilityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a maid with skills, pricing and weekly availability windows")
    public MaidResponse add(@Valid @RequestBody AddMaidRequest request) {
        return ApiMapper.toResponse(maidService.onboard(ApiMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a maid's profile")
    public MaidResponse get(@PathVariable String id) {
        return ApiMapper.toResponse(maidService.get(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Discover maids",
            description = "Give `start` (and optionally `durationMinutes`, default 60) to get only maids who are "
                    + "free for that slot; omit `start` to just browse. Every other query parameter is handled by "
                    + "a pluggable filter; unknown parameters are ignored. Results are ordered by rating, then price.")
    @Parameters({
            @Parameter(name = "locality", in = ParameterIn.QUERY, description = "e.g. Koramangala"),
            @Parameter(name = "service", in = ParameterIn.QUERY, description = "CLEANING, COOKING, DISHWASHING, LAUNDRY"),
            @Parameter(name = "skills", in = ParameterIn.QUERY, description = "comma separated; maid must offer all"),
            @Parameter(name = "minRating", in = ParameterIn.QUERY, description = "e.g. 4.5"),
            @Parameter(name = "maxPrice", in = ParameterIn.QUERY, description = "max hourly rate, e.g. 300"),
            @Parameter(name = "gender", in = ParameterIn.QUERY, description = "FEMALE, MALE, OTHER")
    })
    public List<MaidResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(defaultValue = "60") int durationMinutes,
            @Parameter(hidden = true) @RequestParam Map<String, String> params) {
        TimeSlot slot = start == null ? null : TimeSlot.of(start, durationMinutes);
        List<Maid> maids = searchService.search(new MaidSearchQuery(slot, params));
        return maids.stream().map(ApiMapper::toResponse).toList();
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Free intervals of a maid on a date (working hours minus existing bookings)")
    public List<TimeSlotResponse> freeSlots(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Maid maid = maidService.get(id);
        return availabilityService.freeSlots(maid, date).stream().map(ApiMapper::toResponse).toList();
    }
}
