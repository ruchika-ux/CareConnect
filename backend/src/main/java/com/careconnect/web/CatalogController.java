package com.careconnect.web;

import com.careconnect.dto.Dtos.*;
import com.careconnect.model.*;
import com.careconnect.repository.*;
import com.careconnect.service.MatchingService;
import com.careconnect.service.Views;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/** Service catalog and caregiver discovery. */
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ServiceRepository services; private final UserRepository users; private final ReviewRepository reviews;
    private final Views views; private final CurrentUser current;

    public CatalogController(ServiceRepository services, UserRepository users, ReviewRepository reviews, Views views, CurrentUser current) {
        this.services = services; this.users = users; this.reviews = reviews; this.views = views; this.current = current;
    }

    @GetMapping("/services")
    public List<ServiceView> services() {
        return services.findAll().stream().filter(ServiceCatalog::isEnabled)
                .map(s -> new ServiceView(s.getId(), s.getName(), s.getDescription())).toList();
    }

    /** Verified caregivers; filter by service, area and minimum rating; sort by rating, experience or name. */
    @GetMapping("/caregivers")
    public List<UserView> caregivers(@RequestParam(required = false) Long serviceId, @RequestParam(required = false) String area,
                                     @RequestParam(required = false) Double minRating, @RequestParam(defaultValue = "rating") String sort) {
        ServiceCatalog svc = serviceId == null ? null
                : services.findById(serviceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        Comparator<UserView> order = switch (sort) {
            case "experience" -> Comparator.comparingInt(UserView::experienceYears).reversed();
            case "name" -> Comparator.comparing(UserView::name);
            default -> Comparator.comparingDouble(UserView::rating).reversed();
        };
        return users.findByRole(User.Role.CAREGIVER).stream()
                .filter(User::isVerified).filter(User::isEnabled)
                .filter(c -> svc == null || MatchingService.supports(c, svc))
                .filter(c -> area == null || area.isBlank() || c.getServiceArea().toLowerCase().contains(area.trim().toLowerCase()))
                .map(views::user)
                .filter(v -> minRating == null || v.rating() >= minRating)
                .sorted(order).toList();
    }

    @GetMapping("/caregivers/{id}")
    public CaregiverDetail caregiver(Authentication a, @PathVariable long id) {
        User viewer = current.of(a);
        User c = users.findById(id).filter(u -> u.getRole() == User.Role.CAREGIVER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caregiver not found"));
        boolean privileged = viewer.getRole() == User.Role.ADMIN || viewer.getId().equals(c.getId());
        if (!privileged && (!c.isVerified() || !c.isEnabled())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Caregiver not found");
        return new CaregiverDetail(views.user(c), views.availability(c.getId()),
                reviews.findByCaregiverId(c.getId()).stream().map(views::review).toList());
    }
}
