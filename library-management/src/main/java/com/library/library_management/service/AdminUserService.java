package com.library.library_management.service;

import com.library.library_management.dto.user.UserResponseDTO;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.User;
import com.library.library_management.repository.BorrowRepository;
import com.library.library_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final BorrowRepository borrowRepository;

    public AdminUserService(UserRepository userRepository, BorrowRepository borrowRepository) {
        this.userRepository = userRepository;
        this.borrowRepository = borrowRepository;
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


    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        borrowRepository.deleteByUserId(id);
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
