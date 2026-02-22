package com.library.library_management.repository;

import com.library.library_management.model.Borrow;
import com.library.library_management.model.BorrowStatus;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    //here we get some sql queries for free

    //Count active borrows by user
    long countByUserIdAndStatus(Long userId, BorrowStatus status );

    //check if user already has this book active.
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowStatus status);

    //user viewing their borrow history
    List<Borrow> findByUserId(Long userId);


}
