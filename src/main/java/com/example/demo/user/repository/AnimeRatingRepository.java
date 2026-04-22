package com.example.demo.user.repository;

import com.example.demo.user.entity.User;
import com.example.demo.user.entity.AnimeRating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimeRatingRepository 
        extends JpaRepository<AnimeRating, Long> {
                Optional<AnimeRating> findByMalId(Long malId);
                List<AnimeRating> findByUser(User user);
                Optional<AnimeRating> findByUserAndMalId(User user, Long malId);

    @Query(value = "SELECT r.user.id FROM AnimeRating r " +
            "JOIN Anime a ON r.malId = a.malId " +
            "JOIN AnimeRating my ON r.malId = my.malId " +
            "JOIN Anime myA ON my.malId = myA.malId " +
            "WHERE my.user.id = :myId AND r.user.id != :myId " +
            "GROUP BY r.user.id " +
            "ORDER BY (COUNT(DISTINCT r.malId) * 1.5) + " +
            "SUM(CASE WHEN a.genres LIKE CONCAT('%', myA.genres, '%') THEN 1 ELSE 0 END) DESC")
    List<Long> findSimilarUserIds(@Param("myId") Long myId, Pageable pageable);

    // 2. 추천 애니메이션 ID 찾기: 유사 유저들이 고평점(4점 이상)을 준 작품들
    @Query(value = "SELECT r.malId FROM AnimeRating r " +
            "WHERE r.user.id IN :similarUserIds " +
            "AND r.malId NOT IN :myWatchedIds " +
            "AND r.score >= 4 " +
            "GROUP BY r.malId " +
            "ORDER BY COUNT(r.id) DESC, AVG(r.score) DESC")
    List<Long> findRecommendedAnimeIds(@Param("similarUserIds") List<Long> similarUserIds,
                                       @Param("myWatchedIds") List<Long> myWatchedIds,
                                       Pageable pageable);
}
