package com.example.demo.user.service;

import com.example.demo.user.dto.AnimeDTO;
import com.example.demo.user.dto.GenreDTO;
import com.example.demo.user.dto.JikanResponseDTO;
import com.example.demo.user.entity.Anime;
import com.example.demo.user.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimeService {

    private final AnimeRepository animeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> bannedGenres = Set.of(
            "Hentai",
            "Erotica",
            "Ecchi",
            "Boys Love",
            "Girls Love"
    );

    private final AtomicBoolean running = new AtomicBoolean(false);

    public void saveAnime() {

        if (!running.compareAndSet(false, true)) {
            log.info("Crawler already running");
            return;
        }

        try {

        List<Anime> batch = new ArrayList<>();
        Set<Long> batchIds = new HashSet<>();

        for (int page = 1; ; page++) {

            String url =
                    "https://api.jikan.moe/v4/anime?start_date=2005-01-01&order_by=start_date&sort=asc&page=" + page;

            log.info("Fetching page {}", page);

            JikanResponseDTO response;

            try {
                response = restTemplate.getForObject(url, JikanResponseDTO.class);
            }
            catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Rate limit hit. retrying...");
                sleep(5000);
                page--;
                continue;
            }
            catch (Exception e) {
                log.error("API request failed", e); 
                sleep(2000);
                page--;
                continue;
            }

            if (response == null || response.getData() == null)
                continue;

            for (AnimeDTO dto : response.getData()) {

                if (dto.getType() == null) continue;

                if (dto.getScore() == null) continue;

                if (!batchIds.add(dto.getMalId().longValue()))
                    continue;

                if (animeRepository.existsByMalId(dto.getMalId()))
                    continue;

                String type = dto.getType().toLowerCase();

                if (!(type.equals("tv") ||
                        type.equals("movie") ||
                        type.equals("ova")))
                    continue;

                if (isAdultGenre(dto))
                    continue;

                Anime anime = new Anime();

                anime.setMalId(dto.getMalId());
                anime.setTitle(dto.getTitle());
                anime.setType(dto.getType());
                anime.setScore(dto.getScore());

                if (dto.getImages() != null &&
                        dto.getImages().getJpg() != null) {

                    anime.setImageUrl(
                            dto.getImages().getJpg().getImageUrl()
                    );
                }

                if (dto.getAired() != null) {

                    anime.setStartDate(
                            parseDate(dto.getAired().getFrom())
                    );

                    anime.setEndDate(
                            parseDate(dto.getAired().getTo())
                    );
                }

                anime.setRating(dto.getRating());

                anime.setGenres(joinNames(dto.getGenres()));
                anime.setThemes(joinNames(dto.getThemes()));
                anime.setDemographics(joinNames(dto.getDemographics()));

                batch.add(anime);

                if (batch.size() == 50) {
                    saveBatch(batch);
                    batchIds.clear();
                }
            }

            if (response.getPagination() != null &&
                    !response.getPagination().isHasNextPage())
                break;

            sleep(2000);
        }

        if (!batch.isEmpty())
            saveBatch(batch);

        } finally {
            running.set(false);
        } 
    }

    private void saveBatch(List<Anime> batch) {

        try {
            animeRepository.saveAll(batch);
        }
        catch (Exception e) {
            log.error("Batch save failed", e);
        }

        batch.clear();
    }

    private boolean isAdultGenre(AnimeDTO dto) {

        if (dto.getGenres() == null) return false;

        return dto.getGenres().stream()
                .map(GenreDTO::getName)
                .anyMatch(bannedGenres::contains);
    }

    private String joinNames(List<GenreDTO> list) {

        if (list == null) return "";

        return list.stream()
                .map(GenreDTO::getName)
                .collect(Collectors.joining(", "));
    }

    private LocalDate parseDate(String date) {

        if (date == null || date.length() < 10)
            return null;

        return LocalDate.parse(date.substring(0, 10));
    }

    private void sleep(long millis) {

        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            log.error("Sleep interrupted", e);
        }
    }

    public Page<Anime> searchAnime(String keyword, int page) {

        PageRequest pageable = PageRequest.of(page - 1, 20); // 페이지당 20개

        if (keyword == null || keyword.isEmpty()) {
            return animeRepository.findAll(pageable);
        }

        return animeRepository.findByTitleContaining(keyword, pageable);
    }

    public Anime findById(Long id) {
        return animeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("애니 없음"));
    }
}