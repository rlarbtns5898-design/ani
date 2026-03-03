package com.example.demo.user.repository;

import com.example.demo.user.entity.AnimeList;
import com.example.demo.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimeListRepository extends JpaRepository<AnimeList, Long> {

    boolean existsByUserAndAnimeId(User user, Long animeId);

    List<AnimeList> findByUser(User user);
}
