package com.example.demo.user.service;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserAnimeRating;
import com.example.demo.user.repository.AnimeRepository;
import com.example.demo.user.repository.UserAnimeRatingRepository;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;
    private final UserAnimeRatingRepository ratingRepository;

    public void rateAnime(String username, Integer malId, Integer score) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Anime anime = animeRepository.findByMalId(malId)
                .orElseThrow(() -> new RuntimeException("Anime not found"));

        UserAnimeRating rating = ratingRepository
                .findByUserAndAnime(user, anime)
                .orElse(null);

        if (rating == null) {
            rating = UserAnimeRating.builder()
                    .user(user)
                    .anime(anime)
                    .score(score)
                    .build();
        } else {
            rating.setScore(score);
        }

        ratingRepository.save(rating);
    }
}