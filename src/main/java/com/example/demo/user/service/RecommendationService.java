package com.example.demo.user.service;

import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.AnimeRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final AnimeRatingRepository animeRatingRepository;
    private final AnimeRepository animeRepository;

    public List<Anime> getRecommendations(Long myId) {
        System.out.println("=== 취향 반전 페널티 적용 IDF 로직 시작 (UserID: " + myId + ") ===");

        // 1. 내 시청 기록 파악
        List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
        if (myRatings.isEmpty()) {
            System.out.println("RESULT: 내 기록이 없어 추천을 중단합니다.");
            return Collections.emptyList();
        }

        Set<Long> myWatchedIds = myRatings.stream()
                .map(AnimeRating::getMalId).collect(Collectors.toSet());

        // ✅ 내가 "좋아하는" 장르 (7점 이상)
        Set<String> myGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null && r.getScore() >= 7)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        // ✅ [핵심 추가] 내가 "싫어하는" 장르 추출 (4점 이하)
        Set<String> dislikedGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null && r.getScore() <= 4)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        // 후보군 추출을 위한 Top 장르 (기존 로직 유지)
        List<String> myTopGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 2. DB에서 후보 유저 추출
        List<Long> candidateIds = animeRatingRepository.findCandidateUserIds(myId, PageRequest.of(0, 500));

        if (candidateIds.isEmpty() && !myTopGenres.isEmpty()) {
            String g1 = myTopGenres.get(0);
            String g2 = myTopGenres.size() > 1 ? myTopGenres.get(1) : g1;
            String g3 = myTopGenres.size() > 2 ? myTopGenres.get(2) : g1;
            candidateIds = animeRatingRepository.findCandidateUserIdsByTopGenres(myId, g1, g2, g3, PageRequest.of(0, 500));
        }

        if (candidateIds.isEmpty()) return Collections.emptyList();

        // IDF 사전 준비
        List<Object[]> usageData = animeRatingRepository.findAllAnimeUsageCounts();
        long totalUserCount = animeRatingRepository.countDistinctUserIds();

        Map<Long, Double> animeIdfMap = usageData.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> Math.log((double) totalUserCount / ((Long) row[1] + 1)) + 1.0
        ));

        // 3. 후보 유저 데이터 조회
        List<AnimeRating> allCandidateRatings = animeRatingRepository.findAllByUserIds(candidateIds);
        Map<Long, List<AnimeRating>> ratingsByUser = allCandidateRatings.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        // 4. Java에서 정밀 점수 계산 (페널티 로직 추가)
        List<Long> similarUserIds = candidateIds.stream()
                .map(userId -> {
                    List<AnimeRating> theirRatings = ratingsByUser.getOrDefault(userId, Collections.emptyList());
                    if (theirRatings.isEmpty()) return new UserScore(userId, 0.0);

                    // (1) 긍정 유사도: 겹치는 작품의 IDF 합계
                    double commonIdfScore = theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId()))
                            .mapToDouble(r -> animeIdfMap.getOrDefault(r.getMalId(), 1.0))
                            .sum();

                    // (2) 🔥 취향 반전 페널티: 내가 싫어하는 장르를 이 유저가 좋아한다면(4점 이상) 감점
                    double penalty = theirRatings.stream()
                            .filter(r -> r.getAnime() != null && r.getScore() >= 7)
                            .map(r -> r.getAnime().getGenres())
                            .filter(Objects::nonNull)
                            .mapToDouble(g -> {
                                long matchCount = Arrays.stream(g.split(","))
                                        .map(String::trim)
                                        .filter(dislikedGenres::contains)
                                        .count();
                                return matchCount * 1.5; // 싫어하는 장르 하나당 1.5점 감점
                            }).sum();

                    double commonRatio = (double) theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId())).count() / theirRatings.size();

                    double genreMatchRate = theirRatings.stream()
                            .filter(r -> r.getAnime() != null)
                            .map(r -> r.getAnime().getGenres())
                            .filter(Objects::nonNull)
                            .map(g -> Arrays.stream(g.split(",")).map(String::trim).anyMatch(myGenres::contains) ? 1.0 : 0.0)
                            .mapToDouble(Double::doubleValue).average().orElse(0.0);

                    // 최종 점수 계산: 페널티 반영
                    double finalScore = (commonIdfScore * 5.0) + (commonRatio * 15.0) + (genreMatchRate * 10.0) - penalty;
                    return new UserScore(userId, finalScore);
                })
                .sorted(Comparator.comparing(UserScore::getScore).reversed())
                .limit(30)
                .map(UserScore::getUserId).toList();

        // 5 & 6. 최종 추천작 추출 및 변환
        List<Long> recommendedIds = animeRatingRepository.findRecommendedAnimeIds(
                similarUserIds, new ArrayList<>(myWatchedIds), PageRequest.of(0, 10));

        List<Anime> unsortedAnimes = animeRepository.findAllByMalIdIn(recommendedIds);

        return recommendedIds.stream()
                .map(id -> unsortedAnimes.stream()
                        .filter(a -> a.getMalId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull).toList();
    }

    @Getter
    @RequiredArgsConstructor
    private static class UserScore {
        private final Long userId;
        private final double score;
    }
}