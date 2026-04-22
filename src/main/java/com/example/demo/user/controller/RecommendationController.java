package com.example.demo.user.controller;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.User;
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

    /**
     * 세션 기반 현재 유저 추천 목록 가져오기
     * GET /api/recommendations/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<Anime>> getMyRecommendations(@AuthenticationPrincipal User user) {
        // 1. 세션에 로그인 정보가 있는지 확인
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 서비스를 통해 추천 데이터 가져오기
        List<Anime> recommendations = recommendationService.getRecommendations(user);

        // 3. 결과 반환
        return ResponseEntity.ok(recommendations);
    }
}