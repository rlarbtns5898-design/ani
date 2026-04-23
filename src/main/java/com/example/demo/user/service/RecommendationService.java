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
        System.out.println("=== 인기작 페널티 + 탐험 로직 적용 시작 (UserID: " + myId + ") ===");

        // 1. 내 시청 기록 파악
        List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
        if (myRatings.isEmpty()) return Collections.emptyList();

        Set<Long> myWatchedIds = myRatings.stream()
                .map(AnimeRating::getMalId).collect(Collectors.toSet());

        // 선호/비선호 장르 분석
        Set<String> myGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null && r.getScore() >= 7)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        Set<String> dislikedGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null && r.getScore() <= 4)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        // 2. 후보 유저 추출
        List<Long> candidateIds = animeRatingRepository.findCandidateUserIds(myId, PageRequest.of(0, 500));
        if (candidateIds.isEmpty()) return Collections.emptyList();

        // IDF 준비
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

        // 4. 유사 유저 정밀 점수 계산
        List<Long> similarUserIds = candidateIds.stream()
                .map(userId -> {
                    List<AnimeRating> theirRatings = ratingsByUser.getOrDefault(userId, Collections.emptyList());
                    if (theirRatings.isEmpty()) return new UserScore(userId, 0.0);

                    double commonIdfScore = theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId()))
                            .mapToDouble(r -> animeIdfMap.getOrDefault(r.getMalId(), 1.0)).sum();

                    double penalty = theirRatings.stream()
                            .filter(r -> r.getAnime() != null && r.getScore() >= 7)
                            .map(r -> r.getAnime().getGenres())
                            .filter(Objects::nonNull)
                            .mapToDouble(g -> Arrays.stream(g.split(",")).map(String::trim).filter(dislikedGenres::contains).count() * 1.5)
                            .sum();

                    double finalScore = (commonIdfScore * 5.0) - penalty; // 가중치 요약
                    return new UserScore(userId, finalScore);
                })
                .sorted(Comparator.comparing(UserScore::getScore).reversed())
                .limit(30)
                .map(UserScore::getUserId).toList();

        // 5. 일반 추천 후보 (인기작 페널티 쿼리 사용) - 8개 추출
        List<Long> recommendedIds = animeRatingRepository.findRecommendedAnimeIds(
                similarUserIds, new ArrayList<>(myWatchedIds), PageRequest.of(0, 8));

        // 6. [탐험 로직] 숨은 명작 2개 추출
        List<Anime> explorationAnimes = new ArrayList<>();
        if (!myGenres.isEmpty()) {
            List<String> myGenreList = new ArrayList<>(myGenres);
            Collections.shuffle(myGenreList);
            explorationAnimes = animeRepository.findHiddenGemsByGenre(myGenreList.get(0), 2);
        }

        // 7. 최종 결과 믹스
        List<Anime> finalResult = new ArrayList<>();
        if (!recommendedIds.isEmpty()) {
            finalResult.addAll(animeRepository.findAllByMalIdIn(recommendedIds));
        }

        // 탐험 데이터 중복 제거 후 추가
        for (Anime gem : explorationAnimes) {
            if (finalResult.stream().noneMatch(a -> a.getMalId().equals(gem.getMalId()))) {
                finalResult.add(gem);
            }
        }

        // 8. 무작위 셔플 (탐험작이 섞이도록)
        Collections.shuffle(finalResult);

        return finalResult.stream().limit(10).toList();
    }

    @Getter
    @RequiredArgsConstructor
    private static class UserScore {
        private final Long userId;
        private final double score;
    }
}