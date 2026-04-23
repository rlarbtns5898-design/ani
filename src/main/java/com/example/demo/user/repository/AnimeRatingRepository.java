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
    List<AnimeRating> findByUserId(Long userId);
    // 1. 후보군 추출: 나랑 겹치는 작품이 있는 유저 상위 100명만 빠르게 가져옴
    @Query("SELECT r.user.id FROM AnimeRating r " +
            "WHERE r.user.id != :myId " +
            "AND r.malId IN (SELECT my.malId FROM AnimeRating my WHERE my.user.id = :myId) " +
            "GROUP BY r.user.id " +
            "ORDER BY COUNT(r.id) DESC")
    List<Long> findCandidateUserIds(@Param("myId") Long myId, Pageable pageable);

    // 2. 계산을 위해 선택된 유저들의 모든 평점 데이터를 한 번에 가져옴 (N+1 방지)
    @Query("SELECT r FROM AnimeRating r JOIN FETCH r.user JOIN FETCH r.anime WHERE r.user.id IN :userIds")
    List<AnimeRating> findAllByUserIds(@Param("userIds") List<Long> userIds);

    // 3. 최종 추천 애니메이션 ID 찾기 (기존 유지)
    @Query("SELECT r.malId FROM AnimeRating r " +
            "WHERE r.user.id IN :similarUserIds " +
            "AND r.malId NOT IN :myWatchedIds " +
            "AND r.score >= 4 " +
            "GROUP BY r.malId " +
            "ORDER BY COUNT(r.id) DESC, AVG(r.score) DESC")
    List<Long> findRecommendedAnimeIds(@Param("similarUserIds") List<Long> similarUserIds,
                                       @Param("myWatchedIds") List<Long> myWatchedIds,
                                       Pageable pageable);
}