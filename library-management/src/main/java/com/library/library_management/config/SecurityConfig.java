package com.library.library_management.config;

import com.library.library_management.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the application.
 *
 * @Configuration — tells Spring "this class contains bean definitions, read it on startup."
 *
 * This class defines HOW requests are secured and registers
 * shared security utilities (like the password encoder) into Spring's container.
 *
 * Current state: JWT-based security is ACTIVE.
 *   - Public endpoints: auth routes + GET books (anyone can browse)
 *   - Everything else: requires a valid JWT token in the Authorization header
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Our custom JWT filter — injected here so we can plug it into the filter chain
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Constructor injection — Spring gives us the JwtAuthenticationFilter bean.
     * No @Autowired needed because there's only one constructor.
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Defines the security filter chain — every HTTP request passes through
     * this chain BEFORE reaching your controllers.
     * <p>
     * Rules:
     * 1. CSRF DISABLED — we use JWT tokens (stateless, no cookies), so CSRF is irrelevant.
     * 2. STATELESS sessions — no HttpSession created. The JWT in each request IS the session.
     * 3. Authorization rules:
     * - /api/v1/auth/** → OPEN (login/register don't need a token, obviously)
     * - GET /api/v1/books/** → OPEN (anyone can browse books without logging in)
     * - Everything else → AUTHENTICATED (must send a valid JWT in the Authorization header)
     * 4. JWT filter runs BEFORE Spring's default UsernamePasswordAuthenticationFilter,
     * so by the time Spring checks "is this request authenticated?", our filter
     * has already parsed the token and set the user in SecurityContext.
     *
     * @param http Spring's HttpSecurity builder — fluent API to configure security rules
     * @return the built SecurityFilterChain that Spring applies to every request
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // No sessions — JWT handles identity on every request
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )


                .authorizeHttpRequests(auth -> auth
                        // Public — no token needed
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                        .requestMatchers("/error").permitAll()//to allow for other 403 mappings to pass through
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Everything else requires a valid token
                        .anyRequest().authenticated()
                )

                // Run our JWT filter before Spring's default authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource));


        return http.build();
    }


    /**
     * BCrypt password encoder — hashes passwords with random salt.
     * Registered as a @Bean so any class can inject it via constructor.
     * <p>
     * WHY BCrypt? → It's slow ON PURPOSE (makes brute-force attacks impractical),
     * adds a random salt per password, and is the industry standard.
     *
     * @return a BCryptPasswordEncoder instance managed by Spring's container
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}