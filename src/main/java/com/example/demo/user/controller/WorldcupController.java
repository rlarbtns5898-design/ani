package com.example.demo.user.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.repository.AnimeRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WorldcupController {

    private final AnimeRepository animeRepository;

    @GetMapping("/worldcup")
    public List<Anime> getRandomAnime(@RequestParam int size) {

        List<Anime> all = animeRepository.findAll();

        // size 예외 처리
        if (size > all.size()) {
            size = all.size();
        }

        Collections.shuffle(all);

        return all.subList(0, size);
    }
}