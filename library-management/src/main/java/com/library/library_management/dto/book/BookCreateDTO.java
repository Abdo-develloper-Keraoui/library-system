package com.library.library_management.dto.book;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookCreateDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 50)
    private String title;

    @NotBlank(message = "Author is required")
    @Size(min = 1, max = 50)
    private String author;


    @NotBlank(message = "ISBN is required")
    @Size(min = 10, max = 13)
    private String isbn;


    @NotNull(message = "Publication year is required")
    @Min(value = -3000, message = "Publication year must be valid (after 3000 BC)")
    private Integer pubYear;


    @NotNull(message = "Copies available is required")
    @Min(value = 1, message = "At least one copy is required")
    private Integer copiesAvailable;

    private String coverImageUrl; // nullable — no validation needed

    private String genre;
}
