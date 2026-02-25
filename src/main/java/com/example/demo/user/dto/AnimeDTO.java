package com.example.demo.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimeDTO {
    private String title;
    private Double score;
    private String type;
    private AiredDTO aired;
}
