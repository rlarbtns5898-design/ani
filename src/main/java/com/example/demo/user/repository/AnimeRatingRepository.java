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
            "WHERE r.malId IN :myWatchedIds AND r.user.id != :myId " +
            "GROUP BY r.user.id " +
            "ORDER BY COUNT(r.id) DESC")
    List<Long> findSimilarUserIds(@Param("myWatchedIds") List<Long> myWatchedIds,
                                  @Param("myId") Long myId,
                                  Pageable pageable);

    // 2. 그들이 높은 점수를 줬지만 나는 아직 안 본 애니메이션 추천
    @Query(value = "SELECT r.malId FROM AnimeRating r " +
            "WHERE r.user.id IN :similarUserIds " +
            "AND r.malId NOT IN :myWatchedIds " +
            "AND r.score >= 4 " +
            "GROUP BY r.malId " +
            "ORDER BY AVG(r.score) DESC, COUNT(r.id) DESC")
    List<Long> findRecommendedAnimeIds(@Param("similarUserIds") List<Long> similarUserIds,
                                       @Param("myWatchedIds") List<Long> myWatchedIds,
                                       Pageable pageable);
}
