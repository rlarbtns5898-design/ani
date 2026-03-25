package com.example.demo.user.controller;

import com.example.demo.user.service.AnimeService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnimedbController {

    private final AnimeService animeService;

    @GetMapping("/api/fetch-all")
public ResponseEntity<?> fetchAll() {

    new Thread(() -> animeService.fetchAllAnime()).start();

    return ResponseEntity.ok(
            Map.of("message", "백그라운드 크롤링 시작됨")
    );
}
}
