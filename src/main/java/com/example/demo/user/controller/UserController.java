package com.example.demo.user.controller;

import com.example.demo.user.dto.UserDTO;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
public ResponseEntity<?> register(@RequestBody UserDTO dto) {

    userService.register(dto.getUsername(), dto.getPassword(), dto.getAge(), dto.getGender());
    return ResponseEntity.ok("회원가입 성공");
}

@GetMapping("/me")
public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails user) {
    if (user == null) {
        return ResponseEntity.status(401).build(); // ⭐ 핵심
    }
    return ResponseEntity.ok(user.getUsername());
}
}
