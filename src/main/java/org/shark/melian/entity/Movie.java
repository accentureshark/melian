package org.shark.melian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for Movie data following Spring Data best practices.
 */
@Entity
@Table(name = "movies", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"title", "source"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    private String overview;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(precision = 3, scale = 1)
    private Double rating;

    @Column(length = 50)
    private String source;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Movie(String title, String overview, String releaseDate, Double rating, String source) {
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.source = source;
    }
}