package com.example.demo.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class AnimeReview {

    @Id @GeneratedValue
    private Long id;

    private String content;

    private Long malId;

    @ManyToOne
    private User user; // 작성자
    
}
