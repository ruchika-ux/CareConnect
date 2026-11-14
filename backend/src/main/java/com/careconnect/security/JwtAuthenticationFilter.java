package com.careconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsService users;
    private final byte[] secret;
    public JwtAuthenticationFilter(UserDetailsService users, @Value("${app.jwt.secret}") String secret) {
        this.users=users; this.secret=secret.getBytes(StandardCharsets.UTF_8);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")&&SecurityContextHolder.getContext().getAuthentication()==null) {
            try {
                Claims claims=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret)).build().parseSignedClaims(header.substring(7)).getPayload();
                var principal=users.loadUserByUsername(claims.getSubject());
                if(!principal.isEnabled()) throw new IllegalStateException("disabled");
                var authentication=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request,response);
    }
}
