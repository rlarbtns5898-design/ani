package com.example.demo.user.controller;

import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(Authentication authentication ) {
    if (authentication != null && authentication.isAuthenticated()) {
        return "redirect:/home";
    }
    return "login";
}

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam Integer age,
                           @RequestParam String gender) {

        userService.register(username, password,age,gender);
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
