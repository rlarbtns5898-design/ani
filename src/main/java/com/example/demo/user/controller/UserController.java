package com.example.demo.user.controller;

import com.example.demo.user.dto.UserDTO;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
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
}
