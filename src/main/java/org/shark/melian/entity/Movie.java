package org.shark.melian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "film")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "film_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(name = "description")
    private String overview;

    @Column(name = "release_year")
    private String releaseDate;

    @Column(name = "imdb_rating", precision = 3, scale = 1)
    private BigDecimal rating; // Cambiado a BigDecimal

    @Column(name = "director", length = 100)
    private String director;

    @Column(name = "last_update")
    private Timestamp lastUpdate;
}