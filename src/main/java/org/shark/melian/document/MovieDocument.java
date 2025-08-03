package org.shark.melian.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB Document for Movie data following Spring Data MongoDB best practices.
 */
@Document(collection = "movies")
@CompoundIndex(def = "{'title': 1, 'source': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDocument {

    @Id
    private String id;

    private String title;
    private String overview;
    private String releaseDate;
    private Double rating;
    private String source;

    @CreatedDate
    private LocalDateTime createdAt;

    public MovieDocument(String title, String overview, String releaseDate, Double rating, String source) {
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.source = source;
    }
}