package com.example.demo.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.user.entity.AnimeReview;

public interface AnimeReviewRepository extends JpaRepository<AnimeReview, Long> {
    List<AnimeReview> findByMalId(Long malId);
}
