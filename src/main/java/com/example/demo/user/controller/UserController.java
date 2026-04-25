package com.example.demo.user.controller;

import com.example.demo.user.dto.UserDTO;
import com.example.demo.user.entity.User;
import com.example.demo.user.security.CustomUserDetails;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO dto) {
        try {
            // 회원가입 실행 (중복 시 내부에서 IllegalArgumentException 발생)
            userService.register(dto.getUsername(), dto.getPassword(), dto.getAge(), dto.getGender());
            return ResponseEntity.ok("회원가입 성공");
        } catch (IllegalArgumentException e) {
            // 중복된 아이디 등 사용자 입력 오류 시 400 에러와 메시지 반환
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 그 외 예상치 못한 에러는 500 에러 반환
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 로그인하지 않은 상태일 경우 처리
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        User user = userDetails.getUser();

        // 프론트엔드에서 페이지 이동(온보딩 vs 메인)을 결정할 수 있도록 정보를 넘겨줍니다.
        return ResponseEntity.ok().body(Map.of(
                "username", user.getUsername(),
                "firstLogin", user.isFirstLogin(),
                "age", user.getAge(),
                "gender", user.getGender()
        ));
    }

}





