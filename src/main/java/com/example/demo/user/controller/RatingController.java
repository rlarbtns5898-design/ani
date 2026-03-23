package com.example.demo.user.controller;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.entity.User;

@RestController
@RequiredArgsConstructor
public class RatingController {

    private final AnimeRatingRepository ratingRepository;
    private final UserRepository userRepository;

    @PostMapping("/rate")
    public ResponseEntity<?> rateAnime(@RequestBody Map<String, Object> body) {

        Long malId = Long.valueOf(body.get("malId").toString());
        int score = Integer.parseInt(body.get("score").toString());

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String username = auth.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        Optional<AnimeRating> optionalRating =
                ratingRepository.findByUserAndMalId(user, malId);

        AnimeRating rating;

        if (optionalRating.isPresent()) {
            rating = optionalRating.get();
            rating.setScore(score);
        } else {
            rating = new AnimeRating();
            rating.setUser(user);
            rating.setMalId(malId);
            rating.setScore(score);
        }

        ratingRepository.save(rating);

        // 🔥 JSON 응답
        return ResponseEntity.ok(Map.of(
                "message", "평점 저장 완료",
                "score", score
        ));
    }
}