package com.example.demo.user.controller;

import com.example.demo.user.dto.MyPageAnimeDTO;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.repository.AnimeRatingRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController // 🔥 변경
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final AnimeRatingRepository ratingRepository;

    @GetMapping("/api/mypage") // 🔥 API 경로로 변경
    public List<MyPageAnimeDTO> myPage() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String username = auth.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        List<AnimeRating> ratings = ratingRepository.findByUser(user);

        RestTemplate restTemplate = new RestTemplate();

        List<MyPageAnimeDTO> animeList = new ArrayList<>();

        for (AnimeRating r : ratings) {
            try {
                String url =
                        "https://api.jikan.moe/v4/anime/" + r.getMalId();

                Map response =
                        restTemplate.getForObject(url, Map.class);

                if (response == null || response.get("data") == null)
                    continue;

                Map data = (Map) response.get("data");

                MyPageAnimeDTO dto = new MyPageAnimeDTO();

                dto.setMalId(r.getMalId());
                dto.setTitle((String) data.get("title"));

                Map images = (Map) data.get("images");
                Map jpg = (Map) images.get("jpg");

                dto.setImageUrl((String) jpg.get("image_url"));

                dto.setScore(r.getScore());

                animeList.add(dto);

                Thread.sleep(300);

            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {

                System.out.println("Rate limit 걸림, malId=" + r.getMalId());
                continue; // 🔥 계속 돌지 말고 중단

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return animeList;
    }
}