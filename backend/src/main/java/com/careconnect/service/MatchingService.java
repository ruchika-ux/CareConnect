package com.careconnect.service;

import com.careconnect.dto.Dtos.MatchView;
import com.careconnect.model.*;
import com.careconnect.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Rule-based matching (see spec): service 35, availability 30, area 15, experience 10, rating 10.
 * Service match, verification and availability are hard filters; area, experience and rating rank the rest.
 */
@Service
public class MatchingService {
    private final UserRepository users;
    private final SchedulingService scheduling;
    private final Views views;

    public MatchingService(UserRepository users, SchedulingService scheduling, Views views) {
        this.users = users; this.scheduling = scheduling; this.views = views;
    }

    public static boolean supports(User c, ServiceCatalog s) {
        return Views.serviceNames(c).stream().anyMatch(n -> n.equalsIgnoreCase(s.getName()));
    }

    static boolean servesArea(User c, String location) {
        String area = c.getServiceArea() == null ? "" : c.getServiceArea().trim().toLowerCase();
        String loc = location == null ? "" : location.trim().toLowerCase();
        return !area.isEmpty() && !loc.isEmpty() && (loc.contains(area) || area.contains(loc));
    }

    public List<MatchView> match(CareRequest r) {
        return users.findByRole(User.Role.CAREGIVER).stream()
                .filter(User::isVerified).filter(User::isEnabled)
                .filter(c -> supports(c, r.getService()))
                .filter(c -> scheduling.unavailableReason(c, r).isEmpty())
                .map(c -> evaluate(c, r))
                .sorted(Comparator.<MatchView>comparingInt(MatchView::score).reversed()
                        .thenComparing(m -> -m.caregiver().rating()))
                .toList();
    }

    /** Scores a caregiver that is already known to be eligible. */
    public MatchView evaluate(User c, CareRequest r) {
        var uv = views.user(c);
        Map<String, Integer> b = new LinkedHashMap<>();
        List<String> reasons = new ArrayList<>();
        b.put("Service", 35); reasons.add("Offers " + r.getService().getName());
        b.put("Availability", 30); reasons.add("Free for your requested time");
        boolean area = servesArea(c, r.getLocation());
        b.put("Area", area ? 15 : 0);
        reasons.add(area ? "Serves " + c.getServiceArea() : "Based in " + (c.getServiceArea().isBlank() ? "another area" : c.getServiceArea()));
        int exp = Math.min(10, c.getExperienceYears());
        b.put("Experience", exp);
        reasons.add(c.getExperienceYears() > 0 ? c.getExperienceYears() + " years of experience" : "Early-career caregiver");
        int rating;
        if (uv.reviewCount() == 0) { rating = 5; reasons.add("New on CareConnect — no reviews yet"); }
        else { rating = (int) Math.round(uv.rating() / 5.0 * 10); reasons.add("Rated " + uv.rating() + " / 5 from " + uv.reviewCount() + " review(s)"); }
        b.put("Rating", rating);
        int score = b.values().stream().mapToInt(Integer::intValue).sum();
        return new MatchView(uv, score, b, reasons);
    }
}
