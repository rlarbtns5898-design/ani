package com.example.demo.user.dto;

import java.util.List;

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
    private String rating;
    private String status;
    private AiredDTO aired;
    

    private ImagesDTO images;

    private List<GenreDTO> genres;

    private List<GenreDTO> themes;

    private List<GenreDTO> demographics;
}
