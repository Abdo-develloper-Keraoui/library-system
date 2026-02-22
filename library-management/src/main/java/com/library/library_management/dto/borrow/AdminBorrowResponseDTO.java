package com.library.library_management.dto.borrow;

import com.library.library_management.model.BorrowStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminBorrowResponseDTO {

    private Long id;

    private String bookTitle;

    private String bookAuthor;

    private String bookIsbn;

    private int bookPubYear;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private BorrowStatus status;
    //user info
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;

}
