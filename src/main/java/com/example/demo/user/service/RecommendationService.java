package com.example.demo.user.service;
import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AnimeRatingRepository ratingRepository;
    private final AnimeRepository animeRepository;

    public List<Anime> getRecommendations(User user) {
        // 1. 내가 본 애니메이션 및 평점 정보 가져오기
        List<AnimeRating> myRatings = ratingRepository.findByUser(user);
        if (myRatings.isEmpty()) {
            return animeRepository.findTop10ByOrderByScoreDesc();
        }

        List<Long> myWatchedIds = myRatings.stream().map(AnimeRating::getMalId).toList();

        // 2. [추가] 별점을 반영한 나의 장르별 선호 점수 맵 생성 📊
        Map<String, Double> preferenceMap = getGenrePreferenceScores(myRatings);

        // 3. 유사 유저 찾기 (50명)
        List<Long> similarUserIds = ratingRepository.findSimilarUserIds(
                user.getId(), PageRequest.of(0, 50));

        if (!similarUserIds.isEmpty()) {
            // 4. 추천 후보군 추출 (40개)
            List<Long> recommendedIds = ratingRepository.findRecommendedAnimeIds(
                    similarUserIds, myWatchedIds, PageRequest.of(0, 40));

            if (!recommendedIds.isEmpty()) {
                List<Anime> candidates = animeRepository.findAllByMalIdIn(recommendedIds);

                // 5. [수정] 내 취향 점수를 기준으로 정렬 (Shuffle 대신 가중치 정렬) ⚖️
                return candidates.stream()
                        .sorted((a1, a2) -> {
                            double score1 = calculateFinalScore(a1, preferenceMap);
                            double score2 = calculateFinalScore(a2, preferenceMap);
                            return Double.compare(score2, score1); // 높은 점수 순
                        })
                        .limit(10) // 최종 10개 반환
                        .toList();
            }
        }

        return animeRepository.findTop10ByOrderByScoreDesc();
    }

    // ⭐ 사용자의 별점을 반영하여 장르별 가중치를 계산하는 메서드
    private Map<String, Double> getGenrePreferenceScores(List<AnimeRating> myRatings) {
        Map<String, Double> genreScores = new HashMap<>();

        for (AnimeRating rating : myRatings) {
            // 각 평점에 해당하는 애니메이션의 장르 정보 확인
            animeRepository.findByMalId(rating.getMalId()).ifPresent(anime -> {
                if (anime.getGenres() != null) {
                    String[] genres = anime.getGenres().split(",");
                    double userScore = rating.getScore(); // 사용자가 준 별점 (예: 1~5점)

                    for (String g : genres) {
                        String trimmed = g.trim();
                        // 장르별로 별점 누적 (많이 보고, 높게 평가할수록 점수가 높아짐)
                        genreScores.put(trimmed, genreScores.getOrDefault(trimmed, 0.0) + userScore);
                    }
                }
            });
        }
        return genreScores;
    }

    // ⭐ 추천 후보의 장르가 사용자의 취향 점수와 얼마나 일치하는지 계산
    private double calculateFinalScore(Anime anime, Map<String, Double> preferenceMap) {
        if (anime.getGenres() == null) return 0.0;

        String[] genres = anime.getGenres().split(",");
        double totalScore = 0.0;

        for (String g : genres) {
            // 해당 애니메이션의 장르들이 나의 선호도 맵에 있다면 점수 합산
            totalScore += preferenceMap.getOrDefault(g.trim(), 0.0);
        }
        return totalScore;
    }
}