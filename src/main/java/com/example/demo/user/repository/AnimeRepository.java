package com.example.demo.user.repository;

import com.example.demo.user.entity.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long> {
    boolean existsByTitle(String title);
    Optional<Anime> findByMalId(Long malId);
    Page<Anime> findByTitleContaining(String keyword, Pageable pageable);
    boolean existsByMalId(Long malId);
    @Query("select a.malId from Anime a")
    List<Long> findAllMalIds();
    List<Anime> findTop10ByOrderByScoreDesc();
    @Query(value = "SELECT * FROM anime a " +
            "WHERE a.score >= 7.5 " +
            "AND a.scored_by BETWEEN 1000 AND 50000 " + // 너무 적지도, 너무 많지도 않은 숨은 명작
            "AND (:genreKeyword IS NULL OR a.genres LIKE %:genreKeyword%) " +
            "ORDER BY RANDOM() LIMIT :limitCount", nativeQuery = true)
    List<Anime> findHiddenGemsByGenre(
            @Param("genreKeyword") String genreKeyword,
            @Param("limitCount") int limitCount);
    // 2. 추천 리스트에 포함된 malId들에 해당하는 애니메이션 정보들 가져오기
    List<Anime> findAllByMalIdIn(List<Long> malIds);
}
