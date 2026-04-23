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
            "Hentai",
            "Erotica",
            "Ecchi",
            "Boys Love"
    );

    private final AtomicBoolean running = new AtomicBoolean(false);

public List<Anime> getRandomAnime(int count) {

        List<Anime> animeList = animeRepository.findAll();
        Collections.shuffle(animeList);

        if (animeList.size() <= count) {
            return animeList;
        }

        return animeList.subList(0, count);
}
    @Async
public void fetchAllAnime() {

    if (!running.compareAndSet(false, true)) {
        log.info("이미 실행 중");
        return;
    }

    try {

        Set<Long> existingIds =
                new HashSet<>(animeRepository.findAllMalIds());

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

        String url =
                "https://api.jikan.moe/v4/anime?start_date=2005-01-01&order_by=start_date&sort=asc&page=" + page;

        log.info("Fetching page {}", page);

        JikanResponseDTO response;

        try {
            response = restTemplate.getForObject(url, JikanResponseDTO.class);
        }
        catch (HttpClientErrorException.TooManyRequests e) {
            sleep(4000);
            page--;
            continue;
        }
        catch (Exception e) {
            sleep(2000);
            page--;
            continue;
        }

        if (response == null || response.getData() == null)
            continue;

        for (AnimeDTO dto : response.getData()) {

            if (dto.getType() == null) continue;
            if (dto.getScore() == null) continue;

            Long malId = dto.getMalId().longValue();

            if (existingIds.contains(malId))
                continue;

            String type = dto.getType().toLowerCase();

            if (!(type.equals("tv") ||
                    type.equals("movie") ||
                    type.equals("ova")))
                continue;

            if (isAdultGenre(dto))
                continue;

            Anime anime = new Anime();
            anime.setMalId(dto.getMalId().longValue());
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


            anime.setGenres(joinNames(dto.getGenres()));
            anime.setThemes(joinNames(dto.getThemes()));
            anime.setDemographics(joinNames(dto.getDemographics()));


            anime.setRating(dto.getRating());

            batch.add(anime);
            existingIds.add(malId);

            if (batch.size() == 50) {
                saveBatch(batch);
            }
        }

        if (response.getPagination() != null &&
                !response.getPagination().isHasNextPage()) {
            hasNextPage = false;
            break;
        }

        sleep(1000);
    }

    if (!batch.isEmpty())
        saveBatch(batch);

    return hasNextPage;
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
    @Async
    public void fillMissingAnimeData(List<Long> missingIds) {
        if (missingIds == null || missingIds.isEmpty()) return;

        log.info("누락된 애니메이션 데이터 보강 시작: {}건", missingIds.size());

        for (Long malId : missingIds) {
            try {
                // 1. 단일 작품 조회를 위한 URL (Jikan API v4)
                String url = "https://api.jikan.moe/v4/anime/" + malId;

                // 2. API 호출 (JikanResponseDTO 대신 단일 객체 응답을 위한 Map 또는 전용 DTO 사용)
                // 여기서는 기존 구조를 참고하여 Map으로 받고 데이터 추출
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response == null || !response.containsKey("data")) continue;

                Map<String, Object> data = (Map<String, Object>) response.get("data");

                // 3. Anime 엔티티 생성 및 설정
                Anime anime = new Anime();
                anime.setMalId(malId);
                anime.setTitle((String) data.get("title"));
                anime.setScore(data.get("score") != null ? Double.parseDouble(data.get("score").toString()) : 0.0);
                anime.setType((String) data.get("type"));
                anime.setRating((String) data.get("rating"));

                // 이미지 처리
                Map<String, Object> images = (Map<String, Object>) data.get("images");
                if (images != null) {
                    Map<String, Object> jpg = (Map<String, Object>) images.get("jpg");
                    if (jpg != null) {
                        anime.setImageUrl((String) jpg.get("image_url"));
                    }
                }

                // 장르 처리 (기존의 joinNames 로직 활용을 위해 변환 필요)
                // 만약 여기서 바로 String으로 조인한다면:
                List<Map<String, String>> genresList = (List<Map<String, String>>) data.get("genres");
                if (genresList != null) {
                    String genres = genresList.stream()
                            .map(g -> g.get("name"))
                            .collect(Collectors.joining(", "));
                    anime.setGenres(genres);
                }

                // 4. 저장
                animeRepository.save(anime);
                log.info("보강 완료: [ID:{}] {}", malId, anime.getTitle());

                // API 속도 제한 준수
                Thread.sleep(1000);

            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("API 요청 한도 초과 (429), 잠시 대기합니다...");
                sleep(4000);
            } catch (Exception e) {
                log.error("ID {} 데이터 보강 중 오류 발생: {}", malId, e.getMessage());
            }
        }
        log.info("모든 누락 데이터 보강 작업 종료");
    }
}