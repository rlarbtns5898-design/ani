package com.example.demo.user.controller;

import com.example.demo.user.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
    private final BoardService boardService;

    @GetMapping("")
    public String boardPage(Model model){
        model.addAttribute("boardList",boardService.findAll());
        return "board";
    }
    @GetMapping("/write")
    public String writePage(){
        return "board_write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @AuthenticationPrincipal UserDetails userDetails) {

        boardService.write(title, content, userDetails.getUsername());
        return "redirect:/board";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails) {

        boardService.delete(id, userDetails.getUsername());
        return "redirect:/board";
    }
}
