package com.careconnect.web;

import com.careconnect.model.User;
import com.careconnect.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users = users; }

    public User of(Authentication a) {
        if (a == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in to continue");
        return users.findByEmail(a.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again"));
    }
}
