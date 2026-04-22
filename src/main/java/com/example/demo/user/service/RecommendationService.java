package com.example.demo.user.service;
import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AnimeRatingRepository ratingRepository;
    private final AnimeRepository animeRepository;

    public List<Anime> getRecommendations(User user) {
        System.out.println("=== 추천 로직 시작: 유저 " + user.getUsername() + " ===");

        // 1. 내가 본 애니메이션 및 평점 정보 가져오기
        List<AnimeRating> myRatings = ratingRepository.findByUser(user);
        List<Long> myWatchedIds = myRatings.stream().map(AnimeRating::getMalId).toList();
        System.out.println("내가 평가한 애니 개수: " + myRatings.size());

        // 2. 별점 반영 장르 선호도 계산
        Map<String, Double> preferenceMap = getGenrePreferenceScores(myRatings);
        System.out.println("나의 장르 선호도 점수: " + preferenceMap);

        // 3. 유사 유저 50명 찾기
        List<Long> similarUserIds = ratingRepository.findSimilarUserIds(
                user.getId(), PageRequest.of(0, 50));

        // 4. 추천 후보 ID 추출 (여기서 변수를 선언해야 에러가 안 납니다!)
        List<Long> recommendedIds = new ArrayList<>();
        if (!similarUserIds.isEmpty()) {
            recommendedIds = ratingRepository.findRecommendedAnimeIds(
                    similarUserIds, myWatchedIds, PageRequest.of(0, 40));
        }

        // 5. 후보군이 있다면 내 취향 점수로 정렬하여 반환
        if (!recommendedIds.isEmpty()) {
            List<Anime> candidates = animeRepository.findAllByMalIdIn(recommendedIds);
            System.out.println("후보군 개수: " + candidates.size());

            return candidates.stream()
                    .sorted((a1, a2) -> {
                        double score1 = calculateFinalScore(a1, preferenceMap);
                        double score2 = calculateFinalScore(a2, preferenceMap);

                        // 디버깅용: 어떤 애니가 몇 점인지 확인
                        System.out.println("애니: " + a1.getTitle() + " | 취향점수: " + score1);

                        return Double.compare(score2, score1); // 높은 점수 순
                    })
                    .limit(10)
                    .toList();
        }

        // 6. 후보가 없으면 평점 순 기본 리스트 반환
        System.out.println("추천 후보가 없어 기본 리스트를 반환합니다.");
        return animeRepository.findTop10ByOrderByScoreDesc();
    }

    private Map<String, Double> getGenrePreferenceScores(List<AnimeRating> myRatings) {
        Map<String, Double> genreScores = new HashMap<>();
        for (AnimeRating rating : myRatings) {
            animeRepository.findByMalId(rating.getMalId()).ifPresent(anime -> {
                if (anime.getGenres() != null) {
                    String[] genres = anime.getGenres().split(",");
                    double userScore = rating.getScore();
                    for (String g : genres) {
                        String trimmed = g.trim();
                        genreScores.put(trimmed, genreScores.getOrDefault(trimmed, 0.0) + userScore);
                    }
                }
            });
        }
        return genreScores;
    }

    private double calculateFinalScore(Anime anime, Map<String, Double> preferenceMap) {
        if (anime.getGenres() == null) return 0.0;
        String[] genres = anime.getGenres().split(",");
        double totalScore = 0.0;
        for (String g : genres) {
            totalScore += preferenceMap.getOrDefault(g.trim(), 0.0);
        }
        return totalScore;
    }
}