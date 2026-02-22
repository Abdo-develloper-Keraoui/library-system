package com.library.library_management.dto.book;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookUpdateDTO {

    @Size(min = 1, max = 50)
    private String title;


    @Size(min = 1, max = 50)
    private String author;



    @Size(min = 10, max = 13)
    private String isbn;


    @Min(value = 1000, message = "Publication year must be valid")
    private Integer pubYear;


    @Min(value = 1, message = "At least one copy is required")
    private Integer copiesAvailable;

    private String coverImageUrl;
}
