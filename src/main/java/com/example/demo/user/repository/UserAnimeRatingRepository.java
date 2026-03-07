package com.example.demo.user.repository;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserAnimeRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAnimeRatingRepository extends JpaRepository<UserAnimeRating, Long> {
    Optional<UserAnimeRating> findByUserAndAnime(User user, Anime anime);
}
