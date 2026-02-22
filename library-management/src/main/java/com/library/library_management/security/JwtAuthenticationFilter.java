package com.library.library_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(CustomUserDetailsService customUserDetailsService,  JwtUtils jwtUtils) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Read the Authorization header from the incoming request
        // Every HTTP request has headers — this one should look like: "Bearer eyJhbG..."
        String authHeader = request.getHeader("Authorization");

        // Step 2: If there's no header, or it doesn't start with "Bearer ", skip this filter entirely
        // This covers public endpoints (login, register, browse books) — they have no token
        // We don't block the request — we just do nothing and pass it along
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // pass the request to the next filter/controller
            return; // stop executing this filter
        }

        // Step 3: Extract the raw token by removing the "Bearer " prefix (7 characters)
        // "Bearer eyJhbG..." → "eyJhbG..."
        String token = authHeader.substring(7);
        // Added: if header was "Bearer " with nothing after it, skip
        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 4: Validate the token — checks signature and expiration
        // If someone sent a fake or expired token, we skip setting auth (they'll get 401 from SecurityConfig)
        if (jwtUtils.validateToken(token)) {

            // Step 5: Extract the email from the token's payload
            String email = jwtUtils.extractEmail(token);
            try {
                // Step 6: Load the full user details from the database using that email
                // This gives Spring Security the user's password hash and roles
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // Step 7: Create an authentication object — this is Spring Security's way of saying "this user is authenticated"
                // Parameters: (who they are, their credentials, their roles)
                // We pass null for credentials — we already verified the JWT, we don't need the password again
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,        // the authenticated user
                        null,               // credentials (not needed after JWT validation)
                        userDetails.getAuthorities() // their roles e.g. ROLE_USER, ROLE_ADMIN
                );
                // Step 8: Write that authentication into Spring Security's context for this request
                // This is the "stamp" — SecurityConfig will read this to decide allow or deny
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (UsernameNotFoundException e) {
                // Token was valid but user no longer exists — skip authentication, SecurityConfig returns 401
            }
        }

        // Step 9: Always continue the filter chain — whether token was valid or not
        // If valid → SecurityContextHolder has auth → SecurityConfig will allow protected endpoints
        // If invalid → SecurityContextHolder is empty → SecurityConfig will return 401
        filterChain.doFilter(request, response);
    }

}
