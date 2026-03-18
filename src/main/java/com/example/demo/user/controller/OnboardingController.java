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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OnboardingController {


    private final AnimeService animeService;
    private final AnimeRatingRepository animeRatingRepository;
    private final UserRepository userRepository;
    @GetMapping("/onboarding")
    public String onboarding(Model model) {

        List<Anime> animeList = animeService.getRandomAnime(5);
        model.addAttribute("animeList", animeList);

        return "onboarding";
    }
    @PostMapping("/onboarding/save")
    public String save(
            @RequestParam List<Long> malId,
            @RequestParam List<Integer> score,
            @RequestParam(required = false) List<String> watched,
            Authentication authentication
    ) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        for (int i = 0; i < malId.size(); i++) {

            Long id = malId.get(i);


            if (watched == null || !watched.contains(String.valueOf(id))) continue;

            Integer userScore = score.get(i);
            if (userScore == null || userScore == 0) continue;


            // 이미 있으면 update, 없으면 insert
            AnimeRating rating = animeRatingRepository
                    .findByUserAndMalId(user, id)
                    .orElse(new AnimeRating());

            rating.setUser(user);
            rating.setMalId(id);
            rating.setScore(score.get(i));

            animeRatingRepository.save(rating);
        }
        user.setFirstLogin(false);
        userRepository.save(user);

        return "redirect:/";
    }
}