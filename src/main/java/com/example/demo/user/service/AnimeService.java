package com.example.demo.user.service;

import com.example.demo.user.dto.AnimeDTO;
import com.example.demo.user.dto.GenreDTO;
import com.example.demo.user.dto.JikanResponseDTO;
import com.example.demo.user.entity.Anime;
import com.example.demo.user.repository.AnimeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimeService {

    private final AnimeRepository animeRepository;
    private final RestTemplate restTemplate;

    private static final Set<String> bannedGenres = Set.of(
            "Hentai", "Erotica", "Ecchi", "Boys Love"
    );

    private final AtomicBoolean running = new AtomicBoolean(false);

    // ✅ 1. 성능 최적화: DB 랜덤 쿼리 호출로 변경
    public List<Anime> getRandomAnime(int count) {
        log.info("랜덤 애니메이션 {}건 조회 요청", count);
        return animeRepository.findRandomAnime(count);
    }

    @Async
    public void fetchAllAnime() {
        if (!running.compareAndSet(false, true)) {
            log.info("이미 실행 중");
            return;
        }

        try {
            Set<Long> existingIds = new HashSet<>(animeRepository.findAllMalIds());
            int start = 1;
            int chunkSize = 50;

            while (true) {
                int end = start + chunkSize - 1;
                log.info("Batch start: {} ~ {}", start, end);
                boolean hasNext = saveAnimeChunk(start, end, existingIds);
                if (!hasNext) {
                    log.info("크롤링 완료");
                    break;
                }
                start += chunkSize;
                sleep(2000);
            }
        } finally {
            running.set(false);
        }
    }

    public boolean saveAnimeChunk(int startPage, int endPage, Set<Long> existingIds) {
        List<Anime> batch = new ArrayList<>();
        boolean hasNextPage = true;

        for (int page = startPage; page <= endPage; page++) {
            String url = "https://api.jikan.moe/v4/anime?start_date=2005-01-01&order_by=start_date&sort=asc&page=" + page;
            log.info("Fetching page {}", page);

            JikanResponseDTO response;
            try {
                response = restTemplate.getForObject(url, JikanResponseDTO.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                sleep(4000); page--; continue;
            } catch (Exception e) {
                sleep(2000); page--; continue;
            }

            if (response == null || response.getData() == null) continue;

            for (AnimeDTO dto : response.getData()) {
                if (dto.getType() == null || dto.getScore() == null) continue;

                Long malId = dto.getMalId().longValue();
                if (existingIds.contains(malId)) continue;

                String type = dto.getType().toLowerCase();
                if (!(type.equals("tv") || type.equals("movie") || type.equals("ova"))) continue;
                if (isAdultGenre(dto)) continue;

                // ✅ 공통 변환 메서드 사용 (아래에 정의)
                Anime anime = convertToEntity(dto);
                batch.add(anime);
                existingIds.add(malId);

                if (batch.size() == 50) saveBatch(batch);
            }

            if (response.getPagination() != null && !response.getPagination().isHasNextPage()) {
                hasNextPage = false;
                break;
            }
            sleep(1000);
        }

        if (!batch.isEmpty()) saveBatch(batch);
        return hasNextPage;
    }

    // ✅ 2. 중복 로직 제거: DTO를 Entity로 변환하는 전용 메서드
    private Anime convertToEntity(AnimeDTO dto) {
        Anime anime = new Anime();
        anime.setMalId(dto.getMalId().longValue());
        anime.setTitle(dto.getTitle());
        anime.setType(dto.getType());
        anime.setScore(dto.getScore());
        anime.setRating(dto.getRating());

        if (dto.getImages() != null && dto.getImages().getJpg() != null) {
            anime.setImageUrl(dto.getImages().getJpg().getImageUrl());
        }

        if (dto.getAired() != null) {
            anime.setStartDate(parseDate(dto.getAired().getFrom()));
            anime.setEndDate(parseDate(dto.getAired().getTo()));
        }

        anime.setGenres(joinNames(dto.getGenres()));
        anime.setThemes(joinNames(dto.getThemes()));
        anime.setDemographics(joinNames(dto.getDemographics()));

        return anime;
    }

    private void saveBatch(List<Anime> batch) {
        try {
            animeRepository.saveAll(batch);
        } catch (Exception e) {
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
        if (date == null || date.length() < 10) return null;
        return LocalDate.parse(date.substring(0, 10));
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { log.error("Sleep interrupted", e); }
    }

    @Async
    public void fillMissingAnimeData(List<Long> missingIds) {
        if (missingIds == null || missingIds.isEmpty()) return;
        log.info("누락된 데이터 보강 시작: {}건", missingIds.size());

        for (Long malId : missingIds) {
            try {
                String url = "https://api.jikan.moe/v4/anime/" + malId;
                // 여기서는 API 응답 구조가 다르므로 별도 처리하거나,
                // 단일 조회를 AnimeDTO로 매핑할 수 있도록 전용 DTO를 쓰는 것이 좋습니다.
                // (일단 기존 로직 유지하되 sleep은 꼭 챙기세요)
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("ID {} 보강 중 오류", malId, e);
            }
        }
    }
}