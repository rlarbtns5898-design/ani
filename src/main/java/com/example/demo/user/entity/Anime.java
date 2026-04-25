package com.example.demo.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="mal_id",unique = true)
    private Long malId;

    private String title;

    private String imageUrl;

    private String type;

    private LocalDate startDate;

    private LocalDate endDate;

    private String rating;

    private Double score;

    private String genres;

    private String themes;

    private String demographics;

    public List<String> getGenreList() {
        if (this.genres == null || this.genres.isEmpty()) return Collections.emptyList();
        return Arrays.stream(this.genres.split(","))
                .map(String::trim)
                .toList();
    }
}