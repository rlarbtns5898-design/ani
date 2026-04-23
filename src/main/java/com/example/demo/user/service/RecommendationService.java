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
        // 1. 내 시청 기록 및 전체 장르 파악
        List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
        if (myRatings.isEmpty()) return Collections.emptyList();

        Set<Long> myWatchedIds = myRatings.stream()
                .map(AnimeRating::getMalId)
                .collect(Collectors.toSet());

        // 전체 장르 셋 (유사도 계산용)
        Set<String> myGenres = myRatings.stream()
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        // 🔥 내 핵심 장르 Top 3 추출 (Fallback용)
        List<String> myTopGenres = myRatings.stream()
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
        // 시도 1: 똑같은 작품을 본 유저 찾기
        List<Long> candidateIds = animeRatingRepository.findCandidateUserIds(myId, PageRequest.of(0, 500));

        // 시도 2: 똑같은 작품 본 유저가 없으면 장르 기반으로 찾기 (Fallback)
        if (candidateIds.isEmpty() && !myTopGenres.isEmpty()) {
            String g1 = myTopGenres.get(0);
            String g2 = myTopGenres.size() > 1 ? myTopGenres.get(1) : g1;
            String g3 = myTopGenres.size() > 2 ? myTopGenres.get(2) : g1;

            candidateIds = animeRatingRepository.findCandidateUserIdsByTopGenres(myId, g1, g2, g3, PageRequest.of(0, 500));
        }

        if (candidateIds.isEmpty()) return Collections.emptyList();

        // 3. 후보 유저들의 평점 데이터를 통째로 조회 (N+1 방지)
        List<AnimeRating> allCandidateRatings = animeRatingRepository.findAllByUserIds(candidateIds);
        Map<Long, List<AnimeRating>> ratingsByUser = allCandidateRatings.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        // 4. Java에서 정밀 점수 계산
        List<Long> similarUserIds = candidateIds.stream()
                .map(userId -> {
                    List<AnimeRating> theirRatings = ratingsByUser.getOrDefault(userId, Collections.emptyList());
                    int totalTheirCount = theirRatings.size();
                    if (totalTheirCount == 0) return new UserScore(userId, 0.0);

                    long commonCount = theirRatings.stream()
                            .filter(r -> myWatchedIds.contains(r.getMalId()))
                            .count();

                    double commonRatio = (double) commonCount / totalTheirCount;

                    double genreMatchRate = theirRatings.stream()
                            .map(r -> r.getAnime().getGenres())
                            .filter(Objects::nonNull)
                            .map(g -> Arrays.stream(g.split(",")).map(String::trim).anyMatch(myGenres::contains) ? 1.0 : 0.0)
                            .mapToDouble(Double::doubleValue)
                            .average().orElse(0.0);

                    // 점수 계산 (공통작품 수 + 공통비율 + 장르일치도)
                    double finalScore = (commonCount * 1.5) + (commonRatio * 20.0) + (genreMatchRate * 10.0);
                    return new UserScore(userId, finalScore);
                })
                .sorted(Comparator.comparing(UserScore::getScore).reversed())
                .limit(30)
                .map(UserScore::getUserId)
                .toList();

        // 5. 정예 멤버 30명의 선호 작품 중 내가 안 본 것 추천
        List<Long> recommendedIds = animeRatingRepository.findRecommendedAnimeIds(
                similarUserIds,
                new ArrayList<>(myWatchedIds),
                PageRequest.of(0, 10)
        );

        if (recommendedIds.isEmpty()) return Collections.emptyList();

        // 6. ID 리스트를 실제 Anime 엔티티 리스트로 변환 (순서 보장)
        List<Anime> unsortedAnimes = animeRepository.findAllById(recommendedIds);

        return recommendedIds.stream()
                .map(id -> unsortedAnimes.stream()
                        .filter(anime -> anime.getMalId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Getter
    @RequiredArgsConstructor
    private static class UserScore {
        private final Long userId;
        private final double score;
    }
}