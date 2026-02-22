package com.library.library_management.dto.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//Data replaces getter and setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookResponseDTO {
    //id, title, author, isbn, pubYear, copiesAvailable, coverImageUrl, createdAt

    private Long id;

    private String title;

    private String author;

    private String isbn;

    private int pubYear;

    private int copiesAvailable;

    private String coverImageUrl;

    //date of creation of book inside my app
    private LocalDateTime createdAt;
}
