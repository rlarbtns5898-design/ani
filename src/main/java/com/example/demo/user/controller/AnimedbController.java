package com.example.demo.user.controller;

import com.example.demo.user.service.AnimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnimedbController {
    private final AnimeService animeService;

    @GetMapping("/fetch-all")
    public String fetchAll() {
        animeService.fetchAllAnime();
        return "전체 크롤링 시작됨";
    }

}
