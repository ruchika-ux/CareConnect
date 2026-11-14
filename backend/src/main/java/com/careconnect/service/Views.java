package com.careconnect.service;

import com.careconnect.dto.Dtos.*;
import com.careconnect.model.*;
import com.careconnect.repository.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Maps entities to API views. */
@Component
public class Views {
    private final ReviewRepository reviews;
    private final ServiceRepository services;
    private final AvailabilityRepository availability;

    public Views(ReviewRepository reviews, ServiceRepository services, AvailabilityRepository availability) {
        this.reviews = reviews; this.services = services; this.availability = availability;
    }

    public static List<String> serviceNames(User u) {
        String s = u.getOfferedServices();
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    public UserView user(User u) {
        List<Review> rs = u.getRole() == User.Role.CAREGIVER ? reviews.findByCaregiverId(u.getId()) : List.of();
        double avg = rs.stream().mapToInt(Review::getRating).average().orElse(0);
        List<String> names = serviceNames(u);
        List<Long> ids = services.findAll().stream().filter(s -> names.stream().anyMatch(s.getName()::equalsIgnoreCase))
                .map(ServiceCatalog::getId).toList();
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getPhone(), u.getBio(),
                u.isAvailable(), u.isVerified(), u.isEnabled(), u.getServiceArea(), u.getExperienceYears(),
                Math.round(avg * 10) / 10.0, rs.size(), u.getDateOfBirth(), ids, names);
    }

    public RequestView request(CareRequest r) {
        List<Review> rv = reviews.findByRequest(r);
        Review review = rv.isEmpty() ? null : rv.get(0);
        User c = r.getCaregiver();
        return new RequestView(r.getId(), r.getPatient().getId(), r.getPatient().getName(), r.getService().getId(),
                r.getService().getName(), r.getRequestedDate(), r.getStartTime(), r.getDurationMinutes(), r.getLocation(),
                r.getCareRequirements(), r.getStatus().name(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                r.getMatchReason(), r.getNotes(), review != null, review == null ? null : review.getRating(),
                review == null ? null : review.getComment());
    }

    public ReviewView review(Review r) {
        return new ReviewView(r.getId(), r.getRequest().getId(), r.getPatient().getName(), r.getCaregiver().getName(),
                r.getRating(), r.getComment());
    }

    public List<AvailabilityView> availability(Long caregiverId) {
        return availability.findByCaregiverId(caregiverId).stream()
                .sorted(Comparator.comparingInt(Availability::getDayOfWeek).thenComparing(Availability::getStartTime))
                .map(a -> new AvailabilityView(a.getDayOfWeek(), a.getStartTime(), a.getEndTime())).toList();
    }
}
