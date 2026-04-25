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
            "AND (:genre IS NULL OR a.genres LIKE CONCAT('%', :genre, '%')) " + // Native Query에서는 CONCAT을 쓰는 게 안전합니다.
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Anime> findHiddenGemsByGenre(
            @Param("genre") String genre,   // 이름을 :genre와 똑같이 "genre"로 맞춤
            @Param("limit") int limit       // :limit과 똑같이 "limit"으로 맞춤
    );
    // 2. 추천 리스트에 포함된 malId들에 해당하는 애니메이션 정보들 가져오기
    List<Anime> findAllByMalIdIn(List<Long> malIds);
    @Query(value = "SELECT * FROM anime ORDER BY RANDOM() LIMIT :size", nativeQuery = true)
    List<Anime> findRandomAnime(@Param("size") int size);
}
