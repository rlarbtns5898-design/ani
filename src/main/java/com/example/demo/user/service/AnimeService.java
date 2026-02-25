package com.example.demo.user.service;

import com.example.demo.user.dto.AnimeDTO;
import com.example.demo.user.dto.JikanResponseDTO;
import com.example.demo.user.entity.Anime;
import com.example.demo.user.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AnimeService {

    private final AnimeRepository animeRepository;

    public void saveRecentAnime() {

        String url = "https://api.jikan.moe/v4/anime";

        RestTemplate restTemplate = new RestTemplate();
        JikanResponseDTO response =
                restTemplate.getForObject(url, JikanResponseDTO.class);

        LocalDate targetDate = LocalDate.of(2023, 1, 1);

        for (AnimeDTO dto : response.getData()) {

            // 1️⃣ 타입 필터
            if (dto.getType() == null) continue;

            String type = dto.getType().toLowerCase();

            if (!(type.equals("tv") ||
                    type.equals("ova") ||
                    type.equals("movie"))) {
                continue;
            }

            // 2️⃣ 방영일 필터
            if (dto.getAired() == null || dto.getAired().getFrom() == null)
                continue;

            LocalDate airedDate =
                    LocalDate.parse(dto.getAired().getFrom().substring(0, 10));

            if (airedDate.isAfter(targetDate)) {

                if (!animeRepository.existsByTitle(dto.getTitle())) {

                    Anime anime = new Anime();
                    anime.setTitle(dto.getTitle());
                    anime.setScore(dto.getScore());
                    anime.setStartDate(airedDate);

                    animeRepository.save(anime);
                }
            }
        }
    }
}