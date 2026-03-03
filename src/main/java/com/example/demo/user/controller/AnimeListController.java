package com.example.demo.user.controller;

import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.entity.AnimeList;
import com.example.demo.user.repository.AnimeListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AnimeListController {

    private final AnimeListRepository animeListRepository;
    private final UserRepository userRepository;

    @PostMapping("/mylist/add")
    public String addToMyList(@RequestParam Long animeId,
                              @RequestParam String title,
                              @RequestParam String imageUrl) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String username = auth.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow();

        // 중복 방지
        if (!animeListRepository.existsByUserAndAnimeId(user, animeId)) {

            AnimeList anime = AnimeList.builder()
                    .animeId(animeId)
                    .title(title)
                    .imageUrl(imageUrl)
                    .user(user)
                    .build();

            animeListRepository.save(anime);
        }

        return "redirect:/anime/" + animeId;
    }

    @PostMapping("/mylist/delete")
public String deleteFromMyList(@RequestParam Long animeId) {

    Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

    String username = auth.getName();

    User user = userRepository
            .findByUsername(username)
            .orElseThrow();

    animeListRepository
            .findByUser(user)
            .stream()
            .filter(a -> a.getAnimeId().equals(animeId))
            .findFirst()
            .ifPresent(animeListRepository::delete);

    return "redirect:/mypage";
}
}