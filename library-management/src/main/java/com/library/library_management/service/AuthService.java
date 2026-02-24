package com.library.library_management.service;


import com.library.library_management.dto.auth.AuthResponseDTO;
import com.library.library_management.dto.auth.LoginDTO;
import com.library.library_management.dto.auth.RegisterDTO;
import com.library.library_management.exception.BusinessException;
import com.library.library_management.model.Role;
import com.library.library_management.model.User;
import com.library.library_management.repository.UserRepository;
import com.library.library_management.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles authentication stuff — registration (and later login).
 * This is where we create new user accounts and hash their passwords
 * so we never store raw passwords in the DB (that would be terrible).
 */

@Service
public class AuthService {

    // We need these two to save users and hash passwords
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;


    /**
     * Constructor injection — Spring gives us the UserRepository and
     * the BCryptPasswordEncoder we defined in SecurityConfig.
     * No @Autowired needed because there's only one constructor.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }


    /**
     * Registers a brand new user in the system.
     *
     * What happens step by step:
     *   1. Check if the email is already taken → if yes, throw an error (no duplicates!)
     *   2. Create a fresh User entity
     *   3. Copy fields from the DTO into the entity
     *      - PASSWORD IS HASHED with BCrypt before saving (never store plain text!!)
     *   4. Every new user gets the role USER by default (not ADMIN — that would be a security hole)
     *   5. Save the user to the database
     *   6. Return a response DTO with email + role (token is null for now — we'll add JWT later)
     *
     * @param dto the registration form data from the client (firstName, lastName, email, password)
     * @return AuthResponseDTO with null token, user's email, and their role
     * @throws BusinessException if someone tries to register with an email that's already taken
     */
    public AuthResponseDTO register(RegisterDTO dto) {

        // Step 1: Check if email exists — we don't want two accounts with the same email
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email already in use");
        }

        // Step 2: Create new User object
        User user = new User();

        // Step 3: Set all fields — notice we ENCODE the password, not store it raw
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // Hash password before saving — never store plain text


        // Step 4: Default role is USER — admin accounts are created differently
        user.setRole(Role.USER);

        // Step 5: Save to database
        userRepository.save(user);

        // Step 6: Return response (token is null for now — JWT comes on a later day)
        return new AuthResponseDTO(null, user.getEmail(), user.getRole().name(),  user.getFirstName());
    }


    public AuthResponseDTO login(LoginDTO dto){
        // Fetch the user — if email not found, wrong credentials
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        //Check if the plain text password matches the stored hash — if not, throw an exception
        if(!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid email or password");
        }

        //create a jwt then return it
        return new AuthResponseDTO(jwtUtils.generateToken(user.getEmail()), user.getEmail(), user.getRole().name(), user.getFirstName());

    }

}
