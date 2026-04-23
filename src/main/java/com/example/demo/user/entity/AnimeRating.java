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
    @JoinColumn(
            name = "malId",                // AnimeRating 테이블 내의 FK 컬럼명
            referencedColumnName = "mal_id", // 참조하는 Anime 테이블의 실제 컬럼명 (중요!)
            insertable = false,
            updatable = false
    )
    private Anime anime;
}