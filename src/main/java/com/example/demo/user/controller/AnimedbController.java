package com.example.demo.user.controller;

import com.example.demo.user.service.AnimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnimedbController {
    private final AnimeService animeService;

    @GetMapping("/anime/save")
    public String saveAnime() {
        animeService.saveRecentAnime();
        return "저장 완료";
    }

}
