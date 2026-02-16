package com.example.demo.user.controller;

import com.example.demo.user.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
    private final BoardService boardService;

    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @AuthenticationPrincipal UserDetails userDetails) {

        boardService.write(title, content, userDetails.getUsername());
        return "redirect:/home";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails) {

        boardService.delete(id, userDetails.getUsername());
        return "redirect:/home";
    }
}
