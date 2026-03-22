package com.example.demo.user.controller;

import com.example.demo.user.dto.AnimeDTO;
import com.example.demo.user.dto.AnimeResponseDTO;
import com.example.demo.user.dto.AnimeSearchCondition;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.repository.AnimeRatingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/anime")
public class AnimeController {
    private final AnimeRatingRepository ratingRepository;

    @GetMapping("/search")
public Map<String, Object> searchAnime(
        AnimeSearchCondition condition,
        @RequestParam(defaultValue = "1") int page) {

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

    if (condition.getOrderBy() != null && !condition.getOrderBy().isEmpty())
        builder.queryParam("order_by", condition.getOrderBy());

    if (condition.getSort() != null && !condition.getSort().isEmpty())
        builder.queryParam("sort", condition.getSort());

    builder.queryParam("page", page);

    RestTemplate restTemplate = new RestTemplate();
    Map response = restTemplate.getForObject(builder.toUriString(), Map.class);

    // 🔥 여기부터 추가
    Map pagination = (Map) response.get("pagination");
    int lastPage = ((Number) pagination.get("last_visible_page")).intValue();

    int pageGroupSize = 5;
    int half = pageGroupSize / 2;

    int startPage = Math.max(1, page - half);
    int endPage = Math.min(lastPage, page + half);

    if (endPage - startPage < pageGroupSize - 1) {
        if (startPage == 1) {
            endPage = Math.min(lastPage, startPage + pageGroupSize - 1);
        } else if (endPage == lastPage) {
            startPage = Math.max(1, endPage - pageGroupSize + 1);
        }
    }

    // 🔥 최종 반환
    return Map.of(
            "data", response.get("data"),
            "pagination", pagination,
            "startPage", startPage,
            "endPage", endPage,
            "currentPage", page,
            "lastPage", lastPage
    );
}

@GetMapping("/{id}")
public Map<String, Object> animeDetail(@PathVariable Long id) {

    String url = "https://api.jikan.moe/v4/anime/" + id;

    RestTemplate restTemplate = new RestTemplate();

    Map response = restTemplate.getForObject(url, Map.class);

    Map data = (Map) response.get("data");

    Optional<AnimeRating> rating = ratingRepository.findByMalId(id);

    Map<String, Object> result = new HashMap<>();
    result.put("anime", data);
    result.put("rating", rating.orElse(null));

    return result;
}

}