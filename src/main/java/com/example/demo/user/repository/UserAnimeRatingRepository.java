package com.example.demo.user.repository;

import com.example.demo.user.entity.UserAnimeRating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnimeRatingRepository extends JpaRepository<UserAnimeRating, Long> {
}
