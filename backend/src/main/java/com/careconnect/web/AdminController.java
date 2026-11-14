package com.careconnect.web;

import com.careconnect.dto.Dtos.UserView;
import com.careconnect.model.CareRequest;
import com.careconnect.model.User;
import com.careconnect.repository.*;
import com.careconnect.service.Views;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users; private final CareRequestRepository requests; private final Views views; private final CurrentUser current;

    public AdminController(UserRepository users, CareRequestRepository requests, Views views, CurrentUser current) {
        this.users = users; this.requests = requests; this.views = views; this.current = current;
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("users", users.count());
        m.put("patients", (long) users.findByRole(User.Role.PATIENT).size());
        List<User> cg = users.findByRole(User.Role.CAREGIVER);
        m.put("caregivers", (long) cg.size());
        m.put("pendingVerification", cg.stream().filter(c -> !c.isVerified()).count());
        m.put("requests", requests.count());
        m.put("awaitingResponse", requests.countByStatus(CareRequest.Status.REQUESTED));
        m.put("active", requests.countByStatus(CareRequest.Status.ACCEPTED) + requests.countByStatus(CareRequest.Status.IN_PROGRESS));
        m.put("completed", requests.countByStatus(CareRequest.Status.COMPLETED));
        m.put("cancelled", requests.countByStatus(CareRequest.Status.CANCELLED) + requests.countByStatus(CareRequest.Status.REJECTED));
        return m;
    }

    @GetMapping("/users")
    public List<UserView> allUsers() { return users.findAll().stream().map(views::user).toList(); }

    /** All caregivers including unverified ones, so they can be reviewed. */
    @GetMapping("/caregivers")
    public List<UserView> caregivers() { return users.findByRole(User.Role.CAREGIVER).stream().map(views::user).toList(); }

    @PutMapping("/caregivers/{id}/verification")
    public UserView verify(@PathVariable long id, @RequestParam boolean verified) {
        User c = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (c.getRole() != User.Role.CAREGIVER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only caregivers can be verified");
        c.setVerified(verified);
        return views.user(users.save(c));
    }

    @PutMapping("/users/{id}/enabled")
    public UserView setEnabled(Authentication a, @PathVariable long id, @RequestParam boolean enabled) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() == User.Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be disabled here");
        u.setEnabled(enabled);
        return views.user(users.save(u));
    }
}
