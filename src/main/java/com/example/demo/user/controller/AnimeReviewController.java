package com.example.demo.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.example.demo.user.dto.AnimeReviewDTO;
import com.example.demo.user.entity.AnimeReview;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeReviewRepository;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class AnimeReviewController {

    private final AnimeReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // ✅ 리뷰 작성
    @PostMapping("/{malId}")
    public ResponseEntity<?> createReview(
            @PathVariable Long malId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        AnimeReview review = new AnimeReview();
        review.setContent(request.get("content"));
        review.setMalId(malId);
        review.setUser(user);

        reviewRepository.save(review);

        return ResponseEntity.ok().build();
    }

    // ✅ 특정 애니 리뷰 조회
    @GetMapping("/{malId}")
    public List<AnimeReviewDTO> getReviews(@PathVariable Long malId) {
        return reviewRepository.findByMalId(malId)
                .stream()
                .map(r -> new AnimeReviewDTO(
                        r.getId(),
                        r.getContent(),
                        r.getUser().getUsername(),
                        r.getMalId()
                ))
                .toList();
    }

    // ✅ 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AnimeReview review = reviewRepository.findById(reviewId).orElseThrow();

        if (!review.getUser().getUsername().equals(userDetails.getUsername())) {
            throw new RuntimeException("권한 없음");
        }

        reviewRepository.delete(review);

        return ResponseEntity.ok().build();
    }

    // ✅ 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AnimeReview review = reviewRepository.findById(reviewId).orElseThrow();

        if (!review.getUser().getUsername().equals(userDetails.getUsername())) {
            throw new RuntimeException("권한 없음");
        }

        review.setContent(request.get("content"));
        reviewRepository.save(review);

        return ResponseEntity.ok().build();
    }
}