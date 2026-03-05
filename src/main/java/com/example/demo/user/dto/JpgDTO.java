package com.example.demo.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JpgDTO {
    @JsonProperty("image_url")
    private String imageUrl;
}
