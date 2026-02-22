package com.library.library_management.service;

import com.library.library_management.dto.borrow.AdminBorrowResponseDTO;
import com.library.library_management.dto.borrow.BorrowResponseDTO;
import com.library.library_management.exception.BusinessException;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.Book;
import com.library.library_management.model.Borrow;
import com.library.library_management.model.BorrowStatus;
import com.library.library_management.model.User;
import com.library.library_management.repository.BookRepository;
import com.library.library_management.repository.BorrowRepository;
import com.library.library_management.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowService {
    // We need these three for all operations!
    private final BorrowRepository borrowRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;



    public BorrowService(BorrowRepository borrowRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.borrowRepository = borrowRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BorrowResponseDTO borrowBook(Long bookId, Long userId) {
        //Fail Fast, exit early!
        // Lock the book row while we check and update availability.
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        // Guard against borrowing when no copies remain.
        if (book.getCopiesAvailable() <= 0) {
            throw new BusinessException("No more copies remain of the book: " + bookId);
        }

        //max 3 active borrows
        if(borrowRepository.countByUserIdAndStatus(userId, BorrowStatus.ACTIVE) >= 3) {
            throw new BusinessException("User cannot have more than three active borrows!");
        }

        //user already has this book borrowed and active
        if(borrowRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, BorrowStatus.ACTIVE)) {
            throw new BusinessException("User cannot borrow the same book twice!");
        }
        //Happy case
        //Create Borrow record
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Borrow borrow = new Borrow();
        borrow.setBook(book);
        borrow.setUser(user);

        //Decrement book copies by 1
        book.setCopiesAvailable(book.getCopiesAvailable()-1);

        bookRepository.save(book);
        Borrow savedBorrow = borrowRepository.save(borrow);

        return mapToDTO(savedBorrow);

    }

    @Transactional
    public BorrowResponseDTO returnBook(Long borrowId, Long userId) {
        //Fail Fast, exit early!
        //Find the borrow record
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found with id: " + borrowId));

        //Check it belongs to this user — if not, throw exception
        if(!borrow.getUser().getId().equals(userId)) {
            throw new BusinessException("User is not the one who borrowed the book!");
        }

        // Check status is ACTIVE — if not, throw exception
        if(!borrow.getStatus().equals(BorrowStatus.ACTIVE)) {
            throw new BusinessException("Book is already returned!");
        }

        //Happy case
        //Set returnDate=now, status=RETURNED
        borrow.setReturnDate(LocalDate.now());
        borrow.setStatus(BorrowStatus.RETURNED);

        //Increment copiesAvailable on book
        Book savedBook = borrow.getBook();
        savedBook.setCopiesAvailable(savedBook.getCopiesAvailable() + 1);

        //Save both — all inside @Transactional
        bookRepository.save(savedBook);
        Borrow savedBorrow = borrowRepository.save(borrow);

        return mapToDTO(savedBorrow);


    }

    public List<BorrowResponseDTO> getMyBorrows(Long userId) {
        return borrowRepository.findByUserId(userId)
                .stream() // opens a pipeline
                .map(this::mapToDTO)
                .toList();
    }

    public List<AdminBorrowResponseDTO> getAllBorrows() {
        return borrowRepository.findAll()
                .stream() // opens a pipeline
                .map(this::mapToAdminDTO)
                .toList();
    }

    // ======================== HELPER METHODS ========================

    /**
     * Converts a Borrow entity → BorrowResponseDTO.
     * Pulls borrow fields directly, and book fields from the nested Book entity.
     *
     * @param borrow the JPA entity from the database
     * @return a DTO safe to send to the client
     */
    private BorrowResponseDTO mapToDTO(Borrow borrow) {
        return new BorrowResponseDTO(
                borrow.getId(),
                borrow.getBook().getTitle(),
                borrow.getBook().getAuthor(),
                borrow.getBook().getIsbn(),
                borrow.getBook().getPubYear(),
                borrow.getBorrowDate(),
                borrow.getDueDate(),
                borrow.getReturnDate(),
                borrow.getStatus()
        );
    }

    private AdminBorrowResponseDTO mapToAdminDTO(Borrow borrow) {
        return new AdminBorrowResponseDTO(
                borrow.getId(),
                borrow.getBook().getTitle(),
                borrow.getBook().getAuthor(),
                borrow.getBook().getIsbn(),
                borrow.getBook().getPubYear(),
                borrow.getBorrowDate(),
                borrow.getDueDate(),
                borrow.getReturnDate(),
                borrow.getStatus(),
                borrow.getUser().getId(),
                borrow.getUser().getFirstName(),
                borrow.getUser().getLastName(),
                borrow.getUser().getEmail()
        );
    }
}
