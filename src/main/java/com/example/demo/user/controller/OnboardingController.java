package com.example.demo.user.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class OnboardingController {

    @GetMapping("/onboarding")
    public String onboarding(){
        return "onboarding";
    }
}
