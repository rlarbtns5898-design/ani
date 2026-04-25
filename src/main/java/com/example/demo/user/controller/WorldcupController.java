package com.example.demo.user.controller;

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
        // DB에서 직접 랜덤하게 size만큼 가져오므로 매우 빠르고 안전합니다.
        // size가 DB의 전체 데이터보다 커도 DB가 알아서 가능한 만큼만 반환합니다.
        return animeRepository.findRandomAnime(size);
    }
}