package com.careconnect.web;

import com.careconnect.dto.Dtos.*;
import com.careconnect.model.User;
import com.careconnect.repository.UserRepository;
import com.careconnect.service.Views;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final AuthenticationManager auth; private final PasswordEncoder encoder;
    private final Views views; private final CurrentUser current; private final String secret; private final long expiry;

    public AuthController(UserRepository users, AuthenticationManager auth, PasswordEncoder encoder, Views views, CurrentUser current,
                          @Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expiry) {
        this.users = users; this.auth = auth; this.encoder = encoder; this.views = views; this.current = current;
        this.secret = secret; this.expiry = expiry;
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterInput in) {
        if (in.role() == User.Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts are managed by the platform");
        String email = in.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        User u = new User(in.name().trim(), email, encoder.encode(in.password()), in.role(),
                in.phone() == null ? "" : in.phone().trim(), in.bio() == null ? "" : in.bio().trim());
        u.setVerified(in.role() == User.Role.PATIENT); // caregivers wait for admin verification
        u.setDateOfBirth(in.dateOfBirth());
        return views.user(users.save(u));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody AuthInput in) {
        Authentication a = auth.authenticate(new UsernamePasswordAuthenticationToken(in.email().trim().toLowerCase(), in.password()));
        User u = users.findByEmail(a.getName()).orElseThrow();
        Date now = new Date();
        String token = Jwts.builder().subject(u.getEmail()).claim("role", u.getRole().name()).issuedAt(now)
                .expiration(new Date(now.getTime() + expiry))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        return Map.of("token", token, "user", views.user(u));
    }

    @GetMapping("/me")
    public UserView me(Authentication a) { return views.user(current.of(a)); }
}
