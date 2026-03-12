package com.example.demo.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JikanResponseDTO {
    private List<AnimeDTO> data;
    private PaginationDTO pagination;

    public List<AnimeDTO> getData() {
        return data;
    }

    public PaginationDTO getPagination() {
        return pagination;
    }
}
