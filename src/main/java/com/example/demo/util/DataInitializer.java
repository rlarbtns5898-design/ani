package com.example.demo.util;

import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.service.AnimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AnimeRatingRepository ratingRepository;
    private final AnimeService animeService; // 기존 AnimeService 주입

    @Override
    public void run(String... args) {
        // 1. 누락된 ID 리스트 조회 (아까 만든 Native Query 메서드)
        List<Long> missingIds = ratingRepository.findMissingAnimeIds();

        if (!missingIds.isEmpty()) {
            // 2. AnimeService에 만들어둔 보강 로직 호출
            animeService.fillMissingAnimeData(missingIds);
        }
    }
}