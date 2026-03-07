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

        for (int page = 1; page <= 5000; page++) {

            String url =
                    "https://api.jikan.moe/v4/anime?start_date=2005-01-01&page=" + page;

            RestTemplate restTemplate = new RestTemplate();
            JikanResponseDTO response =
                    restTemplate.getForObject(url, JikanResponseDTO.class);

            for (AnimeDTO dto : response.getData()) {

                if (dto.getType() == null) continue;

                String type = dto.getType().toLowerCase();

                if (!(type.equals("tv") ||
                        type.equals("ova") ||
                        type.equals("movie"))) continue;

                if (!animeRepository.existsByTitle(dto.getTitle())) {

                    LocalDate airedDate =
                            LocalDate.parse(dto.getAired().getFrom().substring(0, 10));

                    Anime anime = new Anime();
                    anime.setMalId(dto.getMalId());
                    anime.setTitle(dto.getTitle());
                    anime.setScore(dto.getScore());
                    anime.setStartDate(airedDate);

                    animeRepository.save(anime);
                }
            }
        }
    }
}