package com.library.library_management.repository;

import com.library.library_management.model.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    // JpaRepository provides methods like save(), findById(), findAll(), delete(), etc.
    /**Given by JpaRepository
     * findAll()                  → JpaRepository gives you this free
     * findById(id)               → JpaRepository gives you this free
     * save(book)                 → JpaRepository gives you this free (insert + update)
     * deleteById(id)             → JpaRepository gives you this free
     * existsById(id)             → JpaRepository gives you this free
     */

    //lock the book row for update to prevent concurrent borrows
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);
}
