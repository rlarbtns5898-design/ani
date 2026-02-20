package com.example.demo.user.controller;

import org.hibernate.mapping.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;


@Controller
public class AnimeController {
    @GetMapping("/anime")
    public String animeSearchPage() {
        return "anime_search";
    }
    @GetMapping("/anime/search")
    public String searchAnime(@RequestParam String keyword, Model model) {

        String url = "https://api.jikan.moe/v4/anime?q=" + keyword;

        RestTemplate restTemplate = new RestTemplate();
        Map response = restTemplate.getForObject(url, Map.class);

        model.addAttribute("result", response);

        return "anime_result";
    }
}
