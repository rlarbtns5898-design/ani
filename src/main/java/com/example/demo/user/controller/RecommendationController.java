package com.example.demo.user.controller;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.User;
import com.example.demo.user.security.CustomUserDetails;
import com.example.demo.user.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 추가 필요
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/me")
    public ResponseEntity<List<Anime>> getMyRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails) { // 2. 타입을 바꿔야 함!

        // 3. 세션 정보 확인
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 4. userDetails에서 실제 User 객체 꺼내기
        User user = userDetails.getUser();

        // 5. 추천 서비스 호출
        List<Anime> recommendations = recommendationService.getRecommendations(user);

        return ResponseEntity.ok(recommendations);
    }
}