package com.example.demo.user.controller;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.entity.User;
@Controller
@RequiredArgsConstructor
public class RatingController {

    private final AnimeRatingRepository ratingRepository;
    private final UserRepository userRepository;

    @PostMapping("/rate")
    public String rateAnime(@RequestParam Long malId,
                        @RequestParam int score) {

    Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

    String username = auth.getName();

    User user = userRepository
            .findByUsername(username)
            .orElse(null);

    Optional<AnimeRating> optionalRating =
            ratingRepository.findByUserAndMalId(user, malId);

    AnimeRating rating;

    if(optionalRating.isPresent()) {
        rating = optionalRating.get();
        rating.setScore(score);
    } else {
        rating = new AnimeRating();
        rating.setUser(user);
        rating.setMalId(malId);
        rating.setScore(score);
    }

    ratingRepository.save(rating);

    return "redirect:/anime/" + malId;
}
}