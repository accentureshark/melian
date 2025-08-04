package org.shark.melian.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDocument {
    @Id
    private String id;

    private String title;
    private String releaseDate;
    private Object rating;
    private String genre;
    private String overview;
    private String crew;
    private String orig_title;
    private String status;
    private String orig_lang;
    private Double budget_x;
    private Double revenue;
    private String country;


    // Constructor existente para compatibilidad
    public MovieDocument(String title, String overview, String releaseDate, Double rating) {
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;

    }
}