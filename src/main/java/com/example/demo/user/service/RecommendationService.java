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
        System.out.println("=== 하이브리드(작품+장르) 추천 로직 시작 (UserID: " + myId + ") ===");

        // 1. 내 시청 기록 파악
        List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
        if (myRatings.isEmpty()) return Collections.emptyList();

        Set<Long> myWatchedIds = myRatings.stream()
                .map(AnimeRating::getMalId).collect(Collectors.toSet());

        // 선호(7점↑) 및 비선호(4점↓) 장르 추출
        Set<String> myGenres = extractGenresByScore(myRatings, 7, true);
        Set<String> dislikedGenres = extractGenresByScore(myRatings, 4, false);

        // 하이브리드 후보 추출을 위한 Top 3 장르
        List<String> top3Genres = myRatings.stream()
                .filter(r -> r.getAnime() != null)
                .flatMap(r -> Arrays.stream(r.getAnime().getGenres().split(",")).map(String::trim))
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3).map(Map.Entry::getKey).toList();

        // 2. 하이브리드 후보 유저 추출 (Repository의 신규 쿼리 사용)
        String g1 = top3Genres.size() > 0 ? top3Genres.get(0) : "";
        String g2 = top3Genres.size() > 1 ? top3Genres.get(1) : "";
        String g3 = top3Genres.size() > 2 ? top3Genres.get(2) : "";

        List<Long> candidateIds = animeRatingRepository.findHybridCandidateUserIds(
                myId, myWatchedIds, g1, g2, g3, PageRequest.of(0, 500));

        if (candidateIds.isEmpty()) return Collections.emptyList();

        // 3. IDF 및 데이터 준비
        List<Object[]> usageData = animeRatingRepository.findAllAnimeUsageCounts();
        long totalUserCount = animeRatingRepository.countDistinctUserIds();
        Map<Long, Double> animeIdfMap = usageData.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> Math.log((double) totalUserCount / ((Long) row[1] + 1)) + 1.0
        ));

        List<AnimeRating> allCandidateRatings = animeRatingRepository.findAllByUserIds(candidateIds);
        Map<Long, List<AnimeRating>> ratingsByUser = allCandidateRatings.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        // 4. 하이브리드 유사도 정밀 계산 (작품 겹침 + 장르 겹침 가중치)
        List<Long> similarUserIds = candidateIds.stream()
                .map(userId -> {
                    List<AnimeRating> theirRatings = ratingsByUser.getOrDefault(userId, Collections.emptyList());
                    if (theirRatings.isEmpty()) return new UserScore(userId, 0.0);

                    // A. 작품 일치 점수 (나랑 똑같은 걸 봤을 때 - 정밀도)
                    double workScore = theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId()))
                            .mapToDouble(r -> animeIdfMap.getOrDefault(r.getMalId(), 1.0))
                            .sum() * 10.0;

                    // B. 장르 일치 점수 (나랑 취향이 비슷할 때 - 풍성함)
                    double genreScore = theirRatings.stream()
                            .filter(r -> r.getAnime() != null)
                            .mapToDouble(r -> {
                                long match = Arrays.stream(r.getAnime().getGenres().split(","))
                                        .map(String::trim).filter(myGenres::contains).count();
                                return match * 2.0; // 장르 하나당 2점
                            }).average().orElse(0.0) * 5.0;

                    // C. 취향 반전 페널티 (내가 싫어하는 장르를 상대가 좋아할 때)
                    double penalty = theirRatings.stream()
                            .filter(r -> r.getAnime() != null && r.getScore() >= 7)
                            .mapToDouble(r -> Arrays.stream(r.getAnime().getGenres().split(","))
                                    .map(String::trim).filter(dislikedGenres::contains).count() * 1.5)
                            .sum();

                    return new UserScore(userId, workScore + genreScore - penalty);
                })
                .sorted(Comparator.comparing(UserScore::getScore).reversed())
                .limit(30).map(UserScore::getUserId).toList();

        // 5. 일반 추천 후보 추출 (페널티 적용 쿼리)
        List<Long> recommendedIds = animeRatingRepository.findRecommendedAnimeIds(
                similarUserIds, new ArrayList<>(myWatchedIds), PageRequest.of(0, 8));

        // 6. 탐험 로직 (내 최고 선호 장르 중 숨은 명작 2개)
        List<Anime> explorationAnimes = new ArrayList<>();
        if (!top3Genres.isEmpty()) {
            explorationAnimes = animeRepository.findHiddenGemsByGenre(top3Genres.get(0), 2);
        }

        // 7. 최종 결과 믹스
        List<Anime> finalResult = new ArrayList<>();
        if (!recommendedIds.isEmpty()) {
            finalResult.addAll(animeRepository.findAllByMalIdIn(recommendedIds));
        }

        for (Anime gem : explorationAnimes) {
            if (finalResult.size() < 10 && finalResult.stream().noneMatch(a -> a.getMalId().equals(gem.getMalId()))) {
                finalResult.add(gem);
            }
        }

        // 결과가 여전히 부족할 경우를 대비한 셔플 및 반환
        Collections.shuffle(finalResult);
        return finalResult;
    }

    // 장르 추출 헬퍼 메서드
    private Set<String> extractGenresByScore(List<AnimeRating> ratings, int threshold, boolean isAbove) {
        return ratings.stream()
                .filter(r -> r.getAnime() != null && (isAbove ? r.getScore() >= threshold : r.getScore() <= threshold))
                .flatMap(r -> Arrays.stream(r.getAnime().getGenres().split(",")).map(String::trim))
                .collect(Collectors.toSet());
    }

    @Getter
    @RequiredArgsConstructor
    private static class UserScore {
        private final Long userId;
        private final double score;
    }
}