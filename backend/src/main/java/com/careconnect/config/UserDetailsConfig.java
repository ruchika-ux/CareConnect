package com.careconnect.config;

import com.careconnect.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class UserDetailsConfig {
    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email)
                .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                        .password(u.getPasswordHash()).roles(u.getRole().name()).disabled(!u.isEnabled()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
}
