package com.library.library_management.service;

import com.library.library_management.dto.book.BookCreateDTO;
import com.library.library_management.dto.book.BookResponseDTO;
import com.library.library_management.dto.book.BookUpdateDTO;
import com.library.library_management.exception.BusinessException;
import com.library.library_management.exception.ResourceNotFoundException;
import com.library.library_management.model.Book;
import com.library.library_management.repository.BookRepository;

import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

/**
 * Service layer for Book CRUD operations.
 * Acts as the middleman between the controller and the repository.
 * Contains all business logic and validation for books.
 */
@Service
public class BookService {

    // Constructor injection — Spring auto-wires BookRepository here (no need for @Autowired)
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Fetches ALL books from the database.
     * Converts each Book entity → BookResponseDTO using the helper method.
     *
     * Flow: DB → List<Book> → Stream → map each to DTO → List<BookResponseDTO>
     *
     * @return list of all books as DTOs (can be empty, never null)
     */
    public List<BookResponseDTO> getAllBooks() {
        return bookRepository.findAll() // hits the database, returns List<Book>
                .stream() // opens a pipeline
                .map(this::mapToDTO)
                .toList(); // collects back into a List<BookResponseDTO>
    }

    /**
     * Fetches a SINGLE book by its ID.
     * Throws ResourceNotFoundException if no book exists with that ID.
     *
     * @param id the book's primary key
     * @return the matching book as a DTO
     * @throws ResourceNotFoundException if book not found
     */
    public BookResponseDTO getBookById(Long id) {
        return bookRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }




    /**
     * Creates a NEW book in the database.
     * Validates the publication year, maps DTO fields → new Book entity, saves it.
     *
     * @param dto incoming data from the client (title, author, isbn, etc.)
     * @return the saved book as a DTO (now includes generated id and createdAt)
     * @throws BusinessException if pubYear is in the future
     */
    public BookResponseDTO createBook(BookCreateDTO dto) {
        validatePubYear(dto.getPubYear());
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setPubYear(dto.getPubYear().intValue());
        book.setCopiesAvailable(dto.getCopiesAvailable());
        book.setCoverImageUrl(dto.getCoverImageUrl());
        book.setGenre(dto.getGenre());

        Book savedBook = bookRepository.save(book);
        return mapToDTO(savedBook);
    }

    /**
     * PARTIALLY updates an existing book (only non-null fields are applied).
     * Finds the book by ID, patches only the provided fields, saves it back.
     *
     * Why null checks? → This is a PATCH-style update. If the client sends
     * only { "title": "New Title" }, only the title changes — everything else stays.
     *
     * @param id  the book's primary key
     * @param dto fields to update (null fields are skipped)
     * @return the updated book as a DTO
     * @throws ResourceNotFoundException if book not found
     * @throws BusinessException if pubYear is in the future
     */
    public BookResponseDTO updateBook(Long id, BookUpdateDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (dto.getTitle() != null) {
            book.setTitle(dto.getTitle());
        }
        if (dto.getAuthor() != null) {
            book.setAuthor(dto.getAuthor());
        }
        if (dto.getIsbn() != null) {
            book.setIsbn(dto.getIsbn());
        }
        if (dto.getPubYear() != null) {
            validatePubYear(dto.getPubYear());
            book.setPubYear(dto.getPubYear());
        }
        if (dto.getCopiesAvailable() != null) {
            book.setCopiesAvailable(dto.getCopiesAvailable());
        }
        if (dto.getCoverImageUrl() != null) {
            book.setCoverImageUrl(dto.getCoverImageUrl());
        }
        if (dto.getGenre() != null) {
            book.setGenre(dto.getGenre());
        }

        Book updatedBook = bookRepository.save(book);

        return mapToDTO(updatedBook);
    }

    /**
     * DELETES a book by ID.
     * Checks existence first — throws if not found (avoids silent no-ops).
     *
     * @param id the book's primary key
     * @throws ResourceNotFoundException if book not found
     */
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    // ======================== HELPER METHODS ========================

    /**
     * Validates that the publication year is not in the future.
     *
     * @param pubYear the year to validate (null is allowed — skipped)
     * @throws BusinessException if year > current year
     */
    private void validatePubYear(Integer pubYear) {
        if (pubYear != null && pubYear > Year.now().getValue()) {
            throw new BusinessException("Publication year cannot be in the future");
        }
    }

    /**
     * Converts a Book entity → BookResponseDTO.
     * Single place to maintain the mapping — if Book fields change, update only here.
     *
     * @param book the JPA entity from the database
     * @return a DTO safe to send to the client
     */
    private BookResponseDTO mapToDTO(Book book) {
        return new BookResponseDTO( // transforms each Book
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPubYear(),
                book.getCopiesAvailable(),
                book.getCoverImageUrl(),
                book.getGenre(),
                book.getCreatedAt()
        );
    }
}
