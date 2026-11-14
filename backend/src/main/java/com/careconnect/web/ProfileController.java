package com.careconnect.web;

import com.careconnect.dto.Dtos.*;
import com.careconnect.model.*;
import com.careconnect.repository.*;
import com.careconnect.service.Views;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Own profile and (for caregivers) services + weekly availability. */
@RestController
@RequestMapping("/api")
public class ProfileController {
    private final UserRepository users; private final ServiceRepository services; private final AvailabilityRepository availability;
    private final Views views; private final CurrentUser current;

    public ProfileController(UserRepository users, ServiceRepository services, AvailabilityRepository availability, Views views, CurrentUser current) {
        this.users = users; this.services = services; this.availability = availability; this.views = views; this.current = current;
    }

    @GetMapping("/profile")
    public UserView profile(Authentication a) { return views.user(current.of(a)); }

    @PutMapping("/profile") @Transactional
    public UserView update(Authentication a, @RequestBody ProfileInput in) {
        User u = current.of(a);
        boolean caregiver = u.getRole() == User.Role.CAREGIVER;
        if (in.name() != null && !in.name().isBlank()) u.setName(in.name().trim());
        if (in.phone() != null) u.setPhone(in.phone().trim());
        if (in.bio() != null) u.setBio(in.bio().trim());
        if (in.dateOfBirth() != null) {
            if (in.dateOfBirth().isAfter(LocalDate.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth cannot be in the future");
            u.setDateOfBirth(in.dateOfBirth());
        }
        if (caregiver) {
            if (in.serviceArea() != null) u.setServiceArea(in.serviceArea().trim());
            if (in.experienceYears() != null) u.setExperienceYears(Math.max(0, Math.min(60, in.experienceYears())));
            if (in.available() != null) u.setAvailable(in.available());
            if (in.serviceIds() != null) {
                List<String> names = in.serviceIds().stream().distinct().map(id -> services.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found")).getName()).toList();
                u.setOfferedServices(String.join(",", names));
            }
        }
        return views.user(users.save(u));
    }

    @GetMapping("/availability")
    public List<AvailabilityView> myAvailability(Authentication a) {
        User u = requireCaregiver(a);
        return views.availability(u.getId());
    }

    /** Replaces the caregiver's weekly working windows. */
    @PutMapping("/availability") @Transactional
    public List<AvailabilityView> setAvailability(Authentication a, @RequestBody List<@Valid AvailabilityInput> in) {
        User u = requireCaregiver(a);
        for (AvailabilityInput w : in)
            if (w.dayOfWeek() < 1 || w.dayOfWeek() > 7) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Day must be 1 (Mon) to 7 (Sun)");
            else if (!w.endTime().isAfter(w.startTime())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each window must end after it starts");
        availability.deleteByCaregiverId(u.getId());
        availability.flush();
        in.forEach(w -> availability.save(new Availability(u, w.dayOfWeek(), w.startTime(), w.endTime())));
        return views.availability(u.getId());
    }

    private User requireCaregiver(Authentication a) {
        User u = current.of(a);
        if (u.getRole() != User.Role.CAREGIVER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only caregivers have availability");
        return u;
    }
}
