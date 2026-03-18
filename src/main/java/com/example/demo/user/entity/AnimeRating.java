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

    private boolean watched;
    @ManyToOne
    private User user;


}