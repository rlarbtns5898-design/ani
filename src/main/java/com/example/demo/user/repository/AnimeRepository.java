package com.example.demo.user.repository;

import com.example.demo.user.entity.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long> {
    boolean existsByTitle(String title);
    Optional<Anime> findByMalId(Integer malId);
    Page<Anime> findByTitleContaining(String keyword, Pageable pageable);
    boolean existsByMalId(Integer malId);
    @Query("select a.malId from Anime a")
    List<Long> findAllMalIds();
    List<Anime> findTop10ByOrderByScoreDesc();

    // 2. 추천 리스트에 포함된 malId들에 해당하는 애니메이션 정보들 가져오기
    List<Anime> findAllByMalIdIn(List<Long> malIds);
}
