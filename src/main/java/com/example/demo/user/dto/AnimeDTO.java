package com.example.demo.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimeDTO {
    @JsonProperty("mal_id")
    private Integer malId;
    private String title;
    private Double score;
    private String type;
    private AiredDTO aired;
}
