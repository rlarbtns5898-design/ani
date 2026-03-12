package com.example.demo.user.controller;

import com.example.demo.user.dto.AnimeDTO;
import com.example.demo.user.dto.AnimeResponseDTO;
import com.example.demo.user.dto.MyPageAnimeDTO;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.repository.AnimeRatingRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final AnimeRatingRepository ratingRepository;

@GetMapping("/mypage")
public String myPage(Model model) {

    Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

    String username = auth.getName();

    User user = userRepository
            .findByUsername(username)
            .orElse(null);

    List<AnimeRating> ratings = ratingRepository.findByUser(user);

    RestTemplate restTemplate = new RestTemplate();

    List<MyPageAnimeDTO> animeList = new ArrayList<>();

    for (AnimeRating r : ratings) {

        String url =
                "https://api.jikan.moe/v4/anime/" + r.getMalId();

        AnimeResponseDTO response =
                restTemplate.getForObject(url, AnimeResponseDTO.class);

        AnimeDTO anime = response.getData();

        MyPageAnimeDTO dto = new MyPageAnimeDTO();

        dto.setMalId(r.getMalId());
        dto.setTitle(anime.getTitle());
        dto.setImageUrl(anime.getImages().getJpg().getImageUrl());
        dto.setScore(r.getScore());

        animeList.add(dto);
    }

    model.addAttribute("animeList", animeList);

    return "mypage";
}
}