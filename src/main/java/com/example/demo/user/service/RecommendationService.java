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
        try {
            // 1. 내 시청 기록 파악
            List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
            if (myRatings == null || myRatings.isEmpty()) return Collections.emptyList();

            Set<Long> myWatchedIds = myRatings.stream()
                    .map(AnimeRating::getMalId).collect(Collectors.toSet());

            // 장르 추출 (null 세이프 처리)
            Set<String> myGenres = extractGenresByScore(myRatings, 7, true);
            Set<String> dislikedGenres = extractGenresByScore(myRatings, 4, false);

            // Top 3 장르 추출 (에러 방지용 리스트 가공)
            List<String> top3Genres = myRatings.stream()
                    .filter(r -> r.getAnime() != null && r.getAnime().getGenres() != null)
                    .flatMap(r -> Arrays.stream(r.getAnime().getGenres().split(",")).map(String::trim))
                    .filter(g -> !g.isEmpty())
                    .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3).map(Map.Entry::getKey).toList();

            // 2. 하이브리드 후보 유저 추출
            String g1 = top3Genres.size() > 0 ? top3Genres.get(0) : "ALL"; // 빈 값일 경우 대비
            String g2 = top3Genres.size() > 1 ? top3Genres.get(1) : g1;
            String g3 = top3Genres.size() > 2 ? top3Genres.get(2) : g1;

            List<Long> candidateIds = animeRatingRepository.findHybridCandidateUserIds(
                    myId, myWatchedIds, g1, g2, g3, PageRequest.of(0, 500));

            if (candidateIds == null || candidateIds.isEmpty()) return Collections.emptyList();

            // 3. IDF 및 데이터 준비
            List<Object[]> usageData = animeRatingRepository.findAllAnimeUsageCounts();
            long totalUserCount = animeRatingRepository.countDistinctUserIds();

            // IDF 맵 생성 (divide by zero 방지)
            Map<Long, Double> animeIdfMap = usageData.stream().collect(Collectors.toMap(
                    row -> (Long) row[0],
                    row -> Math.log((double) (totalUserCount + 1) / ((Long) row[1] + 1)) + 1.0
            ));

            List<AnimeRating> allCandidateRatings = animeRatingRepository.findAllByUserIds(candidateIds);
            Map<Long, List<AnimeRating>> ratingsByUser = allCandidateRatings.stream()
                    .collect(Collectors.groupingBy(r -> r.getUser().getId()));

            // 4. 유사도 계산
            List<Long> similarUserIds = candidateIds.stream()
                    .map(userId -> {
                        List<AnimeRating> theirRatings = ratingsByUser.getOrDefault(userId, Collections.emptyList());
                        if (theirRatings.isEmpty()) return new UserScore(userId, 0.0);

                        double workScore = theirRatings.stream()
                                .filter(r -> myWatchedIds.contains(r.getMalId()))
                                .mapToDouble(r -> animeIdfMap.getOrDefault(r.getMalId(), 1.0))
                                .sum() * 10.0;

                        double genreScore = theirRatings.stream()
                                .filter(r -> r.getAnime() != null && r.getAnime().getGenres() != null)
                                .mapToDouble(r -> {
                                    long match = Arrays.stream(r.getAnime().getGenres().split(","))
                                            .map(String::trim).filter(myGenres::contains).count();
                                    return match * 2.0;
                                }).average().orElse(0.0) * 5.0;

                        double penalty = theirRatings.stream()
                                .filter(r -> r.getAnime() != null && r.getAnime().getGenres() != null && r.getScore() >= 7)
                                .mapToDouble(r -> Arrays.stream(r.getAnime().getGenres().split(","))
                                        .map(String::trim).filter(dislikedGenres::contains).count() * 1.5)
                                .sum();

                        return new UserScore(userId, workScore + genreScore - penalty);
                    })
                    .sorted(Comparator.comparing(UserScore::getScore).reversed())
                    .limit(30).map(UserScore::getUserId).toList();

            // 5. 추천 애니메이션 ID 조회
            List<Long> recommendedIds = animeRatingRepository.findRecommendedAnimeIds(
                    similarUserIds, new ArrayList<>(myWatchedIds), PageRequest.of(0, 15));

            // 6. 결과 믹스 및 반환
            List<Anime> finalResult = new ArrayList<>();
            if (recommendedIds != null && !recommendedIds.isEmpty()) {
                finalResult.addAll(animeRepository.findAllByMalIdIn(recommendedIds));
            }

            // 탐험 로직
            if (!top3Genres.isEmpty()) {
                List<Anime> gems = animeRepository.findHiddenGemsByGenre(top3Genres.get(0), 3);
                for (Anime gem : gems) {
                    if (finalResult.size() < 10 && finalResult.stream().noneMatch(a -> a.getMalId().equals(gem.getMalId()))) {
                        finalResult.add(gem);
                    }
                }
            }

            Collections.shuffle(finalResult);
            return finalResult.stream().limit(10).toList();

        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에서 실제 에러 원인 확인용
            return Collections.emptyList(); // 에러 발생 시 빈 리스트를 반환하여 500 에러 방지
        }
    }

    private Set<String> extractGenresByScore(List<AnimeRating> ratings, int threshold, boolean isAbove) {
        return ratings.stream()
                .filter(r -> r.getAnime() != null && r.getAnime().getGenres() != null)
                .filter(r -> (isAbove ? r.getScore() >= threshold : r.getScore() <= threshold))
                .flatMap(r -> Arrays.stream(r.getAnime().getGenres().split(",")) // 1. 우선 콤마로만 자른다
                        .map(String::trim) // 2. 앞뒤에 붙은 불필요한 공백(" ")을 제거한다
                        .filter(g -> !g.isEmpty()))
                .collect(Collectors.toSet());
    }

    @Getter
    @RequiredArgsConstructor
    private static class UserScore {
        private final Long userId;
        private final double score;
    }
}