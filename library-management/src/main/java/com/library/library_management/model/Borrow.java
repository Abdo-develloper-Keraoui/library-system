package com.library.library_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "borrows")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;


    @Column(nullable = false)
    private LocalDate borrowDate;

    @Column(nullable = false)
    private LocalDate dueDate;


    private LocalDate returnDate;// no nullable = false — null is valid here

    //due_date what to do here, should borrow handle this business logic add 14 days for example

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BorrowStatus status;

    private static final int BORROW_PERIOD_DAYS  = 14;

    @PrePersist
    protected void onCreate() {
        this.borrowDate = LocalDate.now();
        this.dueDate = this.borrowDate.plusDays(BORROW_PERIOD_DAYS);
        this.status = BorrowStatus.ACTIVE;
    }
}
