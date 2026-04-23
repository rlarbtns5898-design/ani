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
        System.out.println("=== IDF 기반 추천 로직 시작 (UserID: " + myId + ") ===");

        // 1. 내 시청 기록 및 전체 장르 파악
        List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
        if (myRatings.isEmpty()) {
            System.out.println("RESULT: 내 기록이 없어 추천을 중단합니다.");
            return Collections.emptyList();
        }

        Set<Long> myWatchedIds = myRatings.stream()
                .map(AnimeRating::getMalId).collect(Collectors.toSet());

        // 🔥 [추가] 'myGenres' 선언 - 여기서 선언해야 STEP 4에서 사용할 수 있습니다.
        Set<String> myGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        // 🔥 [추가] 후보군이 없을 때를 대비한 Top 장르 추출
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

        // 장르 기반 Fallback (작품 기반 후보가 없을 때)
        if (candidateIds.isEmpty() && !myTopGenres.isEmpty()) {
            String g1 = myTopGenres.get(0);
            String g2 = myTopGenres.size() > 1 ? myTopGenres.get(1) : g1;
            String g3 = myTopGenres.size() > 2 ? myTopGenres.get(2) : g1;
            candidateIds = animeRatingRepository.findCandidateUserIdsByTopGenres(myId, g1, g2, g3, PageRequest.of(0, 500));
        }

        if (candidateIds.isEmpty()) return Collections.emptyList();

        // [신규] IDF 사전 준비
        List<Object[]> usageData = animeRatingRepository.findAllAnimeUsageCounts();
        long totalUserCount = animeRatingRepository.countDistinctUserIds();

        Map<Long, Double> animeIdfMap = usageData.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> {
                    long count = (Long) row[1];
                    // IDF 공식 적용
                    return Math.log((double) totalUserCount / (count + 1)) + 1.0;
                }
        ));

        // 3. 후보 유저 데이터 조회
        List<AnimeRating> allCandidateRatings = animeRatingRepository.findAllByUserIds(candidateIds);
        Map<Long, List<AnimeRating>> ratingsByUser = allCandidateRatings.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        // 4. Java에서 정밀 점수 계산 (IDF 가중치 적용)
        List<Long> similarUserIds = candidateIds.stream()
                .map(userId -> {
                    List<AnimeRating> theirRatings = ratingsByUser.getOrDefault(userId, Collections.emptyList());
                    if (theirRatings.isEmpty()) return new UserScore(userId, 0.0);

                    // 겹치는 작품의 IDF 합계
                    double commonIdfScore = theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId()))
                            .mapToDouble(r -> animeIdfMap.getOrDefault(r.getMalId(), 1.0))
                            .sum();

                    double commonRatio = (double) theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId())).count() / theirRatings.size();

                    // 🔥 이제 myGenres를 정상적으로 참조합니다.
                    double genreMatchRate = theirRatings.stream()
                            .filter(r -> r.getAnime() != null)
                            .map(r -> r.getAnime().getGenres())
                            .filter(Objects::nonNull)
                            .map(g -> Arrays.stream(g.split(",")).map(String::trim).anyMatch(myGenres::contains) ? 1.0 : 0.0)
                            .mapToDouble(Double::doubleValue).average().orElse(0.0);

                    double finalScore = (commonIdfScore * 5.0) + (commonRatio * 15.0) + (genreMatchRate * 10.0);
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