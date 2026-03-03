package com.example.demo.user.controller;

import com.example.demo.user.entity.AnimeList;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeListRepository;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final AnimeListRepository animeListRepository;

    @GetMapping("/mypage")
public String myPage(Model model) {

    Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

    String username = auth.getName();

    User user = userRepository
            .findByUsername(username)
            .orElseThrow();

    List<AnimeList> myList = animeListRepository.findByUser(user);

    model.addAttribute("user", user);
    model.addAttribute("myList", myList);

    return "mypage";
}
}