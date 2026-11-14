package com.careconnect.web;

import com.careconnect.dto.Dtos.*;
import com.careconnect.model.*;
import com.careconnect.repository.*;
import com.careconnect.service.BookingService;
import com.careconnect.service.Views;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/** Care requests / bookings and their lifecycle. */
@RestController
@RequestMapping("/api")
public class RequestController {
    private final CareRequestRepository requests; private final ReviewRepository reviews;
    private final BookingService booking; private final Views views; private final CurrentUser current;

    public RequestController(CareRequestRepository requests, ReviewRepository reviews, BookingService booking, Views views, CurrentUser current) {
        this.requests = requests; this.reviews = reviews; this.booking = booking; this.views = views; this.current = current;
    }

    @GetMapping("/requests")
    public List<RequestView> list(Authentication a, @RequestParam(required = false) String status) {
        User u = current.of(a);
        List<CareRequest> list = switch (u.getRole()) {
            case PATIENT -> requests.findByPatientId(u.getId());
            case CAREGIVER -> requests.findByCaregiverId(u.getId());
            case ADMIN -> requests.findAll();
        };
        return list.stream()
                .filter(r -> status == null || status.isBlank() || r.getStatus().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(CareRequest::getRequestedDate).thenComparing(CareRequest::getStartTime))
                .map(views::request).toList();
    }

    @GetMapping("/requests/{id}")
    public RequestView get(Authentication a, @PathVariable long id) {
        User u = current.of(a);
        CareRequest r = requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        boolean owner = r.getPatient().getId().equals(u.getId());
        boolean assigned = r.getCaregiver() != null && r.getCaregiver().getId().equals(u.getId());
        if (!owner && !assigned && u.getRole() != User.Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        return views.request(r);
    }

    @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(Authentication a, @Valid @RequestBody RequestInput in) {
        return views.request(booking.create(current.of(a), in));
    }

    @GetMapping("/requests/{id}/matches")
    public List<MatchView> matches(Authentication a, @PathVariable long id) { return booking.matches(current.of(a), id); }

    @PostMapping("/requests/{id}/book")
    public RequestView book(Authentication a, @PathVariable long id, @RequestParam long caregiverId) {
        return views.request(booking.book(current.of(a), id, caregiverId));
    }

    @PostMapping("/requests/{id}/decision")
    public RequestView decision(Authentication a, @PathVariable long id, @Valid @RequestBody DecisionInput in) {
        return views.request(booking.decide(current.of(a), id, in.action()));
    }

    @PostMapping("/requests/{id}/cancel")
    public RequestView cancel(Authentication a, @PathVariable long id) { return views.request(booking.cancel(current.of(a), id)); }

    @PostMapping("/requests/{id}/start")
    public RequestView start(Authentication a, @PathVariable long id) { return views.request(booking.start(current.of(a), id)); }

    @PostMapping("/requests/{id}/complete")
    public RequestView complete(Authentication a, @PathVariable long id, @Valid @RequestBody(required = false) VisitInput in) {
        return views.request(booking.complete(current.of(a), id, in == null ? null : in.notes()));
    }

    @PostMapping("/requests/{id}/reviews") @ResponseStatus(HttpStatus.CREATED)
    public ReviewView review(Authentication a, @PathVariable long id, @Valid @RequestBody ReviewInput in) {
        return views.review(booking.review(current.of(a), id, in));
    }

    /** Caregiver: reviews received. Patient: reviews written. Admin: all. */
    @GetMapping("/reviews")
    public List<ReviewView> reviews(Authentication a) {
        User u = current.of(a);
        List<Review> list = switch (u.getRole()) {
            case CAREGIVER -> reviews.findByCaregiverId(u.getId());
            case PATIENT -> reviews.findByPatientId(u.getId());
            case ADMIN -> reviews.findAll();
        };
        return list.stream().map(views::review).toList();
    }
}
