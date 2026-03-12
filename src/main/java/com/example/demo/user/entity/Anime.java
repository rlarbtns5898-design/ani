package com.example.demo.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    private Integer malId;

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

}