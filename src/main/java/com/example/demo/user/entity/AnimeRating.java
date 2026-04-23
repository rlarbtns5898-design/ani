package com.example.demo.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
public class AnimeRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long malId;

    private int score;


    @ManyToOne
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "malId", referencedColumnName = "malId", insertable = false, updatable = false)
    private Anime anime;
}