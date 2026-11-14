package com.careconnect.service;

import com.careconnect.model.*;
import com.careconnect.repository.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

/** Decides whether a caregiver can take a visit: weekly windows + no overlapping live booking. */
@Service
public class SchedulingService {
    public static final Set<CareRequest.Status> LIVE =
            EnumSet.of(CareRequest.Status.REQUESTED, CareRequest.Status.ACCEPTED, CareRequest.Status.IN_PROGRESS);

    private final AvailabilityRepository availability;
    private final CareRequestRepository requests;

    public SchedulingService(AvailabilityRepository availability, CareRequestRepository requests) {
        this.availability = availability; this.requests = requests;
    }

    /** Empty = caregiver is free; otherwise a human-readable reason. The request itself is ignored. */
    public Optional<String> unavailableReason(User c, CareRequest r) {
        if (!c.isEnabled() || !c.isAvailable()) return Optional.of("Caregiver is not taking new visits right now");
        LocalTime start = r.getStartTime(), end = start.plusMinutes(r.getDurationMinutes());
        if (!end.isAfter(start)) return Optional.of("Visits must finish before midnight");
        int dow = r.getRequestedDate().getDayOfWeek().getValue();
        boolean inWindow = availability.findByCaregiverId(c.getId()).stream().anyMatch(a ->
                a.getDayOfWeek() == dow && !start.isBefore(a.getStartTime()) && !end.isAfter(a.getEndTime()));
        if (!inWindow) {
            String day = DayOfWeek.of(dow).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            return Optional.of("Not working " + day + " " + start + "–" + end);
        }
        long self = r.getId() == null ? -1L : r.getId();
        boolean clash = requests.findLive(c.getId(), r.getRequestedDate(), LIVE, self).stream()
                .anyMatch(o -> start.isBefore(o.getStartTime().plusMinutes(o.getDurationMinutes()))
                        && o.getStartTime().isBefore(end));
        return clash ? Optional.of("Already booked for an overlapping visit") : Optional.empty();
    }
}
