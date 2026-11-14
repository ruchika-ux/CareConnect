package com.careconnect.service;

import com.careconnect.dto.Dtos.*;
import com.careconnect.model.*;
import com.careconnect.model.CareRequest.Status;
import com.careconnect.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static com.careconnect.model.CareRequest.Status.*;

/** Booking lifecycle. The state machine and double-booking protection live here, not in the UI. */
@Service
@Transactional
public class BookingService {
    private static final Map<Status, Set<Status>> FLOW = Map.of(
            REQUESTED, Set.of(ACCEPTED, REJECTED, CANCELLED),
            ACCEPTED, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED));

    private final CareRequestRepository requests;
    private final UserRepository users;
    private final ServiceRepository services;
    private final ReviewRepository reviews;
    private final SchedulingService scheduling;
    private final MatchingService matching;
    private final ZoneId zone;

    public BookingService(CareRequestRepository requests, UserRepository users, ServiceRepository services,
                          ReviewRepository reviews, SchedulingService scheduling, MatchingService matching,
                          @Value("${app.timezone:Asia/Kolkata}") String zone) {
        this.requests = requests; this.users = users; this.services = services; this.reviews = reviews;
        this.scheduling = scheduling; this.matching = matching; this.zone = ZoneId.of(zone);
    }

    private static ResponseStatusException err(HttpStatus s, String m) { return new ResponseStatusException(s, m); }

    private CareRequest lock(long id) {
        return requests.lockById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void move(CareRequest r, Status to) {
        if (!FLOW.getOrDefault(r.getStatus(), Set.of()).contains(to))
            throw err(HttpStatus.CONFLICT, "This booking is " + r.getStatus().name().toLowerCase().replace('_', ' ')
                    + " and cannot become " + to.name().toLowerCase().replace('_', ' '));
        r.setStatus(to);
        r.setUpdatedAt(java.time.Instant.now());
    }

    private void requirePatient(User u, CareRequest r) {
        if (u.getRole() != User.Role.PATIENT || !r.getPatient().getId().equals(u.getId())) throw err(HttpStatus.FORBIDDEN, "Not your booking");
    }

    private void requireCaregiver(User u, CareRequest r) {
        if (u.getRole() != User.Role.CAREGIVER || r.getCaregiver() == null || !r.getCaregiver().getId().equals(u.getId()))
            throw err(HttpStatus.FORBIDDEN, "This booking is not assigned to you");
    }

    public CareRequest create(User p, RequestInput in) {
        if (p.getRole() != User.Role.PATIENT) throw err(HttpStatus.FORBIDDEN, "Only patients can request care");
        LocalDateTime now = LocalDateTime.now(zone);
        if (LocalDateTime.of(in.requestedDate(), in.startTime()).isBefore(now.plusMinutes(1)))
            throw err(HttpStatus.BAD_REQUEST, "Choose a future date and time");
        if (!in.startTime().plusMinutes(in.durationMinutes()).isAfter(in.startTime()))
            throw err(HttpStatus.BAD_REQUEST, "Visits must finish before midnight");
        ServiceCatalog s = services.findById(in.serviceId()).filter(ServiceCatalog::isEnabled)
                .orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Service not found"));
        return requests.save(new CareRequest(p, s, in.requestedDate(), in.startTime(), in.durationMinutes(),
                in.location().trim(), in.careRequirements() == null ? "" : in.careRequirements().trim(), ""));
    }

    @Transactional(readOnly = true)
    public List<MatchView> matches(User u, long requestId) {
        CareRequest r = requests.findById(requestId).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Booking not found"));
        if (u.getRole() != User.Role.ADMIN) requirePatient(u, r);
        return matching.match(r);
    }

    public CareRequest book(User p, long requestId, long caregiverId) {
        CareRequest r = lock(requestId);
        requirePatient(p, r);
        if (r.getStatus() != REQUESTED || r.getCaregiver() != null)
            throw err(HttpStatus.CONFLICT, "This request already has a booking");
        // Lock the caregiver row so concurrent bookings for the same caregiver are processed one at a time.
        User c = users.lockById(caregiverId).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Caregiver not found"));
        if (c.getRole() != User.Role.CAREGIVER || !c.isVerified() || !c.isEnabled())
            throw err(HttpStatus.CONFLICT, "Caregiver is not available for booking");
        if (!MatchingService.supports(c, r.getService()))
            throw err(HttpStatus.CONFLICT, "Caregiver does not offer " + r.getService().getName());
        scheduling.unavailableReason(c, r).ifPresent(why -> { throw err(HttpStatus.CONFLICT, "Caregiver is no longer available: " + why); });
        r.setCaregiver(c);
        r.setMatchReason(String.join(" · ", matching.evaluate(c, r).reasons()));
        r.setUpdatedAt(java.time.Instant.now());
        return requests.save(r);
    }

    public CareRequest decide(User c, long id, String action) {
        CareRequest r = lock(id);
        requireCaregiver(c, r);
        if ("ACCEPT".equalsIgnoreCase(action)) {
            users.lockById(c.getId());
            scheduling.unavailableReason(c, r).ifPresent(why -> { throw err(HttpStatus.CONFLICT, "Cannot accept: " + why); });
            move(r, ACCEPTED);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            move(r, REJECTED); // caregiver stays on the record; REJECTED is not a live status so the slot is released
        } else throw err(HttpStatus.BAD_REQUEST, "Choose ACCEPT or REJECT");
        return requests.save(r);
    }

    public CareRequest cancel(User u, long id) {
        CareRequest r = lock(id);
        if (u.getRole() != User.Role.ADMIN) requirePatient(u, r);
        move(r, CANCELLED);
        return requests.save(r);
    }

    public CareRequest start(User c, long id) {
        CareRequest r = lock(id);
        requireCaregiver(c, r);
        move(r, IN_PROGRESS);
        return requests.save(r);
    }

    public CareRequest complete(User c, long id, String notes) {
        CareRequest r = lock(id);
        requireCaregiver(c, r);
        move(r, COMPLETED);
        r.setNotes(notes == null ? "" : notes.trim());
        return requests.save(r);
    }

    public Review review(User p, long id, ReviewInput in) {
        CareRequest r = lock(id);
        requirePatient(p, r);
        if (r.getStatus() != COMPLETED) throw err(HttpStatus.CONFLICT, "Reviews are available after a completed visit");
        if (!reviews.findByRequest(r).isEmpty()) throw err(HttpStatus.CONFLICT, "This visit already has a review");
        return reviews.save(new Review(r, p, r.getCaregiver(), in.rating(), in.comment() == null ? "" : in.comment().trim()));
    }
}
