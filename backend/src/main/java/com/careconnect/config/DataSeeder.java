package com.careconnect.config;

import com.careconnect.model.*;
import com.careconnect.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Seeds the service catalog always; demo users only when app.seed.demo=true; admin when credentials are provided. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final ServiceRepository services; private final UserRepository users; private final AvailabilityRepository availability;
    private final PasswordEncoder encoder;
    private final boolean demo; private final String adminEmail, adminPassword;

    public DataSeeder(ServiceRepository services, UserRepository users, AvailabilityRepository availability, PasswordEncoder encoder,
                      @Value("${app.seed.demo:true}") boolean demo, @Value("${app.admin.email:admin@careconnect.test}") String adminEmail,
                      @Value("${app.admin.password:}") String adminPassword) {
        this.services = services; this.users = users; this.availability = availability; this.encoder = encoder;
        this.demo = demo; this.adminEmail = adminEmail; this.adminPassword = adminPassword;
    }

    @Override @Transactional
    public void run(String... args) {
        Map<String, String> catalog = new LinkedHashMap<>();
        catalog.put("Elderly Care", "Companionship and daily living support at home");
        catalog.put("Home Nursing", "Skilled nursing support in the comfort of home");
        catalog.put("Post-operative Care", "Recovery support after a procedure or hospital stay");
        catalog.put("Patient Companion", "Friendly, attentive non-medical companionship");
        catalog.put("Physiotherapy", "Guided mobility and exercise support at home");
        catalog.forEach((n, d) -> services.findByName(n).orElseGet(() -> services.save(new ServiceCatalog(n, d, "Home care"))));
        String all = String.join(",", catalog.keySet());

        String adminPw = !adminPassword.isBlank() ? adminPassword : (demo ? "demo" : "");
        if (!adminPw.isBlank()) user("Admin", adminEmail, adminPw, User.Role.ADMIN, "", "CareConnect operations", true, "", 0, "");
        if (!demo) return;

        User p = user("Aarav Sharma", "patient@careconnect.test", "demo", User.Role.PATIENT, "+91 98765 43210", "Recovering at home", true, "", 0, "");
        if (p != null) p.setDateOfBirth(LocalDate.of(1958, 4, 12));
        weekly(user("Meera Kapoor", "caregiver@careconnect.test", "demo", User.Role.CAREGIVER, "+91 98765 11111",
                "Certified home-care professional with a focus on thoughtful, reliable support.", true, "Pune", 8, all), 7, 8, 20);
        weekly(user("Sneha Joshi", "sneha@careconnect.test", "demo", User.Role.CAREGIVER, "+91 98765 22222",
                "Registered nurse experienced in post-surgery recovery.", true, "Pune", 5, "Home Nursing,Post-operative Care"), 5, 9, 18);
        weekly(user("Rohan Patil", "rohan@careconnect.test", "demo", User.Role.CAREGIVER, "+91 98765 33333",
                "Physiotherapist awaiting verification.", false, "Mumbai", 3, "Physiotherapy"), 6, 10, 17);
    }

    /** Creates the user if missing; returns the new entity, or null if it already existed. */
    private User user(String name, String email, String pw, User.Role role, String phone, String bio, boolean verified,
                      String area, int years, String offered) {
        if (users.existsByEmail(email)) return null;
        User u = new User(name, email, encoder.encode(pw), role, phone, bio);
        u.setVerified(verified); u.setServiceArea(area); u.setExperienceYears(years); u.setOfferedServices(offered);
        return users.save(u);
    }

    private void weekly(User c, int days, int fromHour, int toHour) {
        if (c == null) return;
        for (int d = 1; d <= days; d++) availability.save(new Availability(c, d, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0)));
    }
}
