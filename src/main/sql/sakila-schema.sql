CREATE
DATABASE IF NOT EXISTS sakila;
USE
sakila;

DROP TABLE IF EXISTS film_actor;
DROP TABLE IF EXISTS film_category;
DROP TABLE IF EXISTS actor;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS film;

CREATE TABLE category
(
    category_id TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(25) NOT NULL,
    last_update TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id)
);

CREATE TABLE actor
(
    actor_id    SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name  VARCHAR(45) NOT NULL,
    last_name   VARCHAR(45) NOT NULL,
    last_update TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id)
);

CREATE TABLE film
(
    film_id              SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title                VARCHAR(255) NOT NULL,
    description          TEXT,
    release_year YEAR,
    language_id          TINYINT UNSIGNED NOT NULL DEFAULT 1,
    original_language_id TINYINT UNSIGNED,
    rental_duration      TINYINT UNSIGNED DEFAULT 3,
    rental_rate          DECIMAL(4, 2)         DEFAULT 4.99,
    length               SMALLINT UNSIGNED,
    replacement_cost     DECIMAL(5, 2)         DEFAULT 19.99,
    rating               VARCHAR(10),
    imdb_rating          DECIMAL(3, 1),
    director             VARCHAR(100),
    budget               BIGINT,
    box_office           BIGINT,
    language             VARCHAR(50),
    last_update          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (film_id)
);

CREATE TABLE film_actor
(
    actor_id    SMALLINT UNSIGNED NOT NULL,
    film_id     SMALLINT UNSIGNED NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id, film_id),
    FOREIGN KEY (actor_id) REFERENCES actor (actor_id),
    FOREIGN KEY (film_id) REFERENCES film (film_id)
);

CREATE TABLE film_category
(
    film_id     SMALLINT UNSIGNED NOT NULL,
    category_id TINYINT UNSIGNED NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (film_id, category_id),
    FOREIGN KEY (film_id) REFERENCES film (film_id),
    FOREIGN KEY (category_id) REFERENCES category (category_id)
);
