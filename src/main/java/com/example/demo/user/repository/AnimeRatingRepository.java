package com.example.demo.user.repository;

import com.example.demo.user.entity.User;
import com.example.demo.user.entity.AnimeRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnimeRatingRepository 
        extends JpaRepository<AnimeRating, Long> {
                Optional<AnimeRating> findByMalId(Long malId);
                List<AnimeRating> findByUser(User user);
                Optional<AnimeRating> findByUserAndMalId(User user, Long malId);
}
