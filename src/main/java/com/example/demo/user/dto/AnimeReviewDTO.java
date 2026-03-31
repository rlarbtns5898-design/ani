package com.example.demo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnimeReviewDTO {
    private Long id;
    private String content;
    private String username;
    private Long malId;
}