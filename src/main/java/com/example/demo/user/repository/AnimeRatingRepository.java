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

    // 1. [기본] 후보군 추출: 나랑 겹치는 작품이 있는 유저 상위 500명 (작품 기반)
    @Query("SELECT r.user.id FROM AnimeRating r " +
            "WHERE r.user.id != :myId " +
            "AND r.malId IN (SELECT my.malId FROM AnimeRating my WHERE my.user.id = :myId) " +
            "GROUP BY r.user.id " +
            "ORDER BY COUNT(r.id) DESC")
    List<Long> findCandidateUserIds(@Param("myId") Long myId, Pageable pageable);

    // 2. [Fallback] 후보군 추출: 내 Top 3 장르 중 하나라도 포함된 작품을 본 유저 (장르 기반) 🎯
    // r.anime와 조인하여 genres 컬럼을 검색합니다.
    @Query("SELECT r.user.id FROM AnimeRating r " +
            "JOIN r.anime a " +
            "WHERE r.user.id != :myId " +
            "AND (" +
            "  a.genres LIKE CONCAT('%', :g1, '%') OR " +
            "  a.genres LIKE CONCAT('%', :g2, '%') OR " +
            "  a.genres LIKE CONCAT('%', :g3, '%')" +
            ") " +
            "GROUP BY r.user.id " +
            "ORDER BY COUNT(r.id) DESC")
    List<Long> findCandidateUserIdsByTopGenres(
            @Param("myId") Long myId,
            @Param("g1") String g1,
            @Param("g2") String g2,
            @Param("g3") String g3,
            Pageable pageable);

    // 3. 계산을 위해 선택된 유저들의 모든 평점 데이터를 한 번에 가져옴 (N+1 방지)
    // 배포 에러 방지를 위해 JOIN FETCH 대상 확인 완료
    @Query("SELECT r FROM AnimeRating r " +
            "JOIN FETCH r.user " +
            "JOIN FETCH r.anime " +
            "WHERE r.user.id IN :userIds")
    List<AnimeRating> findAllByUserIds(@Param("userIds") List<Long> userIds);

    // 4. 최종 추천 애니메이션 ID 찾기
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