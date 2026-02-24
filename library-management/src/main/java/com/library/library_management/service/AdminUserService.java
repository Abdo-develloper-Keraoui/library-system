package com.library.library_management.service;

import com.library.library_management.dto.user.UserResponseDTO;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.User;
import com.library.library_management.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {
    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll() // hits the database, returns List<Book>
                .stream() // opens a pipeline
                .map(this::mapToDTO)
                .toList();
    }

    public UserResponseDTO suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(!user.isActive());
        User savedUser = userRepository.save(user);

        return mapToDTO(savedUser);
    }



    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // ======================== HELPER METHODS ========================

    private UserResponseDTO mapToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}
