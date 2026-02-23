package com.example.demo.user.controller;

import com.example.demo.user.dto.AnimeSearchCondition;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Controller
public class AnimeController {

    @GetMapping("/anime")
    public String animeSearchPage() {
        return "anime_search";
    }

    @GetMapping("/anime/search")
    public String searchAnime(AnimeSearchCondition condition, Model model) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("https://api.jikan.moe/v4/anime");

        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty())
            builder.queryParam("q", condition.getKeyword());

        if (condition.getType() != null && !condition.getType().isEmpty())
            builder.queryParam("type", condition.getType());

        if (condition.getStatus() != null && !condition.getStatus().isEmpty())
            builder.queryParam("status", condition.getStatus());

        if (condition.getRating() != null && !condition.getRating().isEmpty())
            builder.queryParam("rating", condition.getRating());

        if (condition.getMinScore() != null)
            builder.queryParam("min_score", condition.getMinScore());

        if (condition.getStartDate() != null && !condition.getStartDate().isEmpty())
            builder.queryParam("start_date", condition.getStartDate());

        if (condition.getEndDate() != null && !condition.getEndDate().isEmpty())
            builder.queryParam("end_date", condition.getEndDate());

        if (condition.getGenres() != null && !condition.getGenres().isEmpty())
            builder.queryParam("genres", String.join(",", condition.getGenres()));

        if (condition.getThemes() != null && !condition.getThemes().isEmpty())
            builder.queryParam("themes", String.join(",", condition.getThemes()));

        if (condition.getDemographics() != null && !condition.getDemographics().isEmpty())
            builder.queryParam("demographics", String.join(",", condition.getDemographics()));

        if (condition.getOrderBy() != null && !condition.getOrderBy().isEmpty())
            builder.queryParam("order_by", condition.getOrderBy());

        if (condition.getSort() != null && !condition.getSort().isEmpty())
            builder.queryParam("sort", condition.getSort());

        String url = builder.toUriString();

        RestTemplate restTemplate = new RestTemplate();
        Map response = restTemplate.getForObject(url, Map.class);

        model.addAttribute("result", response);

        return "anime_result";
    }
}