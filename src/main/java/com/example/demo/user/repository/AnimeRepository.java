package com.example.demo.user.repository;

import com.example.demo.user.entity.Anime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long> {
    boolean existsByTitle(String title);
    Optional<Anime> findByMalId(Integer malId);
}
