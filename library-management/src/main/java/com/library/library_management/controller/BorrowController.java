package com.library.library_management.controller;

import com.library.library_management.dto.borrow.AdminBorrowResponseDTO;
import com.library.library_management.dto.borrow.BorrowResponseDTO;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.User;
import com.library.library_management.repository.UserRepository;
import com.library.library_management.service.BorrowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/borrows")
public class BorrowController {

    private final BorrowService borrowService;
    private final UserRepository userRepository;


    public BorrowController(BorrowService borrowService, UserRepository userRepository) {
        this.borrowService = borrowService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<List<BorrowResponseDTO>> getMyBorrows(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(borrowService.getMyBorrows(user.getId()));
    }

    /*
        Allow an authenticated user to borrow a specific book (identified by bookId).
        Create a new "borrow" record in the system, linking the user and the book.
        Return details of the created borrow record in the response.
    */

    @PostMapping("/{bookId}/borrow")
    public ResponseEntity<BorrowResponseDTO> borrowBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(borrowService.borrowBook(bookId, user.getId()));
    }

    @PutMapping("/{borrowId}/return")
    public ResponseEntity<BorrowResponseDTO> returnBook(
            @PathVariable Long borrowId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(borrowService.returnBook(borrowId, user.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdminBorrowResponseDTO>> getAllBorrows() {
        List<AdminBorrowResponseDTO> allBorrows = borrowService.getAllBorrows();
        return ResponseEntity.ok(allBorrows);
    }

}
