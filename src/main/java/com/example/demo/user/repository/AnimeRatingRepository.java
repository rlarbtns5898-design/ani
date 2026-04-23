package com.example.demo.user.repository;

import com.example.demo.user.entity.User;
import com.example.demo.user.entity.AnimeRating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnimeRatingRepository extends JpaRepository<AnimeRating, Long> {
    List<AnimeRating> findByUserId(Long userId);

    // [통합] 하이브리드 후보군 추출: 나랑 작품이 겹치거나 선호 장르를 본 유저 500명
    @Query("SELECT DISTINCT r.user.id FROM AnimeRating r " +
            "JOIN r.anime a " +
            "WHERE r.user.id != :myId " +
            "AND (" +
            "  r.malId IN :myWatchedIds OR " +
            "  a.genres LIKE %:g1% OR " +
            "  a.genres LIKE %:g2% OR " +
            "  a.genres LIKE %:g3%" +
            ")")
    List<Long> findHybridCandidateUserIds(
            @Param("myId") Long myId,
            @Param("myWatchedIds") Collection<Long> myWatchedIds,
            @Param("g1") String g1,
            @Param("g2") String g2,
            @Param("g3") String g3,
            Pageable pageable);

    @Query("SELECT ar.malId, COUNT(ar.id) FROM AnimeRating ar GROUP BY ar.malId")
    List<Object[]> findAllAnimeUsageCounts();

    @Query("SELECT COUNT(DISTINCT ar.user.id) FROM AnimeRating ar")
    long countDistinctUserIds();

    @Query("SELECT r FROM AnimeRating r " +
            "JOIN FETCH r.user " +
            "JOIN FETCH r.anime " +
            "WHERE r.user.id IN :userIds")
    List<AnimeRating> findAllByUserIds(@Param("userIds") List<Long> userIds);

    // [수정] HAVING COUNT를 1로 낮춰 데이터가 적어도 결과가 나오게 함
    @Query("SELECT ar.malId " +
            "FROM AnimeRating ar " +
            "WHERE ar.user.id IN :similarUserIds " +
            "AND ar.malId NOT IN :myWatchedIds " +
            "GROUP BY ar.malId " +
            "HAVING COUNT(ar.id) >= 1 " +
            "ORDER BY (AVG(ar.score) * 3.0) - (LOG(COUNT(ar.id) + 1) * 2.0) DESC")
    List<Long> findRecommendedAnimeIds(@Param("similarUserIds") List<Long> similarUserIds,
                                       @Param("myWatchedIds") List<Long> myWatchedIds,
                                       Pageable pageable);
}