package com.example.demo.user.controller;

import com.example.demo.user.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/rate")
    public String rateAnime(
            @RequestParam Integer malId,
            @RequestParam Integer score,
            Authentication authentication
    ) {

        String username = authentication.getName();

        ratingService.rateAnime(username, malId, score);

        return "redirect:/anime/" + malId;
    }
}