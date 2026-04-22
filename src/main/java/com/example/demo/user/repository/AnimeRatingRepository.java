package com.example.demo.user.repository;

import com.example.demo.user.entity.User;
import com.example.demo.user.entity.AnimeRating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimeRatingRepository extends JpaRepository<AnimeRating, Long> {
    Optional<AnimeRating> findByMalId(Long malId);
    List<AnimeRating> findByUser(User user);
    Optional<AnimeRating> findByUserAndMalId(User user, Long malId);

    // 1. 유사 사용자 찾기 쿼리 개선 🎯
    @Query(value = "SELECT r.user.id FROM AnimeRating r " +
            "JOIN Anime a ON r.malId = a.malId " +
            "WHERE r.user.id != :myId " +
            "GROUP BY r.user.id " +
            "ORDER BY " +
            // (1) 나와 같은 작품을 본 횟수 (가중치 2.0)
            "(COUNT(DISTINCT CASE WHEN r.malId IN (SELECT my.malId FROM AnimeRating my WHERE my.user.id = :myId) THEN r.malId END) * 2.0) + " +
            // (2) 내가 평가한 장르들과 겹치는 작품을 본 횟수 (가중치 1.0)
            "SUM(CASE WHEN EXISTS (SELECT 1 FROM AnimeRating my2 JOIN Anime myA ON my2.malId = myA.malId " +
            "WHERE my2.user.id = :myId AND a.genres LIKE CONCAT('%', myA.genres, '%')) THEN 1 ELSE 0 END) DESC")
    List<Long> findSimilarUserIds(@Param("myId") Long myId, Pageable pageable);

    // 2. 추천 애니메이션 ID 찾기 (기존과 동일하되 점수 기준 완화 가능)
    @Query(value = "SELECT r.malId FROM AnimeRating r " +
            "WHERE r.user.id IN :similarUserIds " +
            "AND r.malId NOT IN :myWatchedIds " +
            "AND r.score >= 4 " + // 4점에서  후보군 확보
            "GROUP BY r.malId " +
            "ORDER BY COUNT(r.id) DESC, AVG(r.score) DESC")
    List<Long> findRecommendedAnimeIds(@Param("similarUserIds") List<Long> similarUserIds,
                                       @Param("myWatchedIds") List<Long> myWatchedIds,
                                       Pageable pageable);
}