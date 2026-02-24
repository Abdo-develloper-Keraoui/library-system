package com.library.library_management.repository;

import com.library.library_management.model.Borrow;
import com.library.library_management.model.BorrowStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    //here we get some sql queries for free

    //Count active borrows by user
    long countByUserIdAndStatus(Long userId, BorrowStatus status );

    //check if user already has this book active.
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowStatus status);

    //user viewing their borrow history
    List<Borrow> findByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByBookId(Long bookId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);



}
