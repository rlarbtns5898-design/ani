package com.example.demo.user.controller;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.security.CustomUserDetails;
import com.example.demo.user.service.AnimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final AnimeService animeService;
    private final AnimeRatingRepository animeRatingRepository;
    private final UserRepository userRepository;

    // 🔥 1. 랜덤 애니 가져오기
    @GetMapping
    public List<Map<String, Object>> onboarding() {

        List<Anime> animeList = animeService.getRandomAnime(20);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Anime anime : animeList) {
            Map<String, Object> map = new HashMap<>();
            map.put("malId", anime.getMalId());
            map.put("title", anime.getTitle());
            map.put("imageUrl", anime.getImageUrl());

            result.add(map);
        }

        return result;
    }

    // 🔥 2. 저장
    @PostMapping("/save")
    public Map<String, Object> save(
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        List<Integer> malIds = (List<Integer>) body.get("malId");
        List<Integer> scores = (List<Integer>) body.get("score");
        List<Integer> watched = (List<Integer>) body.get("watched");

        for (int i = 0; i < malIds.size(); i++) {

            Long id = Long.valueOf(malIds.get(i));

            if (watched == null || !watched.contains(malIds.get(i))) continue;

            Integer userScore = scores.get(i);
            if (userScore == null || userScore == 0) continue;

            AnimeRating rating = animeRatingRepository
                    .findByUserAndMalId(user, id)
                    .orElse(new AnimeRating());

            rating.setUser(user);
            rating.setMalId(id);
            rating.setScore(userScore);

            animeRatingRepository.save(rating);
        }

        user.setFirstLogin(false);
        userRepository.save(user);

        return Map.of("message", "온보딩 완료");
    }
}