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
        System.out.println("=== 추천 로직 시작 (UserID: " + myId + ") ===");

        // 1. 내 시청 기록 및 전체 장르 파악
        List<AnimeRating> myRatings = animeRatingRepository.findByUserId(myId);
        System.out.println("STEP 1: 내 시청 기록 수 = " + myRatings.size());

        if (myRatings.isEmpty()) {
            System.out.println("RESULT: 내 기록이 없어 추천을 중단합니다.");
            return Collections.emptyList();
        }

        Set<Long> myWatchedIds = myRatings.stream()
                .map(AnimeRating::getMalId)
                .collect(Collectors.toSet());

        // 🔥 [수정] Anime 객체 null 체크 추가
        Set<String> myGenres = myRatings.stream()
                .filter(r -> r.getAnime() != null)
                .map(r -> r.getAnime().getGenres())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")).map(String::trim))
                .collect(Collectors.toSet());

        // 🔥 [수정] Anime 객체 null 체크 추가
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
        System.out.println("STEP 1: 내 핵심 장르 Top 3 = " + myTopGenres);

        // 2. DB에서 후보 유저 추출
        List<Long> candidateIds = animeRatingRepository.findCandidateUserIds(myId, PageRequest.of(0, 500));
        System.out.println("STEP 2-1 (작품기반): 후보 유저 수 = " + candidateIds.size());

        if (candidateIds.isEmpty() && !myTopGenres.isEmpty()) {
            System.out.println("STEP 2-2: 작품 기반 후보가 없어 장르 기반 Fallback 실행...");
            String g1 = myTopGenres.get(0);
            String g2 = myTopGenres.size() > 1 ? myTopGenres.get(1) : g1;
            String g3 = myTopGenres.size() > 2 ? myTopGenres.get(2) : g1;

            candidateIds = animeRatingRepository.findCandidateUserIdsByTopGenres(myId, g1, g2, g3, PageRequest.of(0, 500));
            System.out.println("STEP 2-2 (장르기반): 후보 유저 수 = " + candidateIds.size());
        }

        if (candidateIds.isEmpty()) {
            System.out.println("RESULT: 후보 유저가 단 한 명도 없어 추천을 중단합니다.");
            return Collections.emptyList();
        }

        // 3. 후보 유저들의 평점 데이터 조회
        List<AnimeRating> allCandidateRatings = animeRatingRepository.findAllByUserIds(candidateIds);
        System.out.println("STEP 3: 후보 유저들의 총 평점 데이터 수 = " + allCandidateRatings.size());

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

                    // 🔥 [수정] Java 계산부에서도 Anime null 체크 추가
                    double genreMatchRate = theirRatings.stream()
                            .filter(r -> r.getAnime() != null)
                            .map(r -> r.getAnime().getGenres())
                            .filter(Objects::nonNull)
                            .map(g -> Arrays.stream(g.split(",")).map(String::trim).anyMatch(myGenres::contains) ? 1.0 : 0.0)
                            .mapToDouble(Double::doubleValue)
                            .average().orElse(0.0);

                    double finalScore = (commonCount * 1.5) + (commonRatio * 20.0) + (genreMatchRate * 10.0);
                    return new UserScore(userId, finalScore);
                })
                .sorted(Comparator.comparing(UserScore::getScore).reversed())
                .limit(30)
                .map(UserScore::getUserId)
                .toList();
        System.out.println("STEP 4: 정밀 필터링 통과 유저(Top 30) 수 = " + similarUserIds.size());

        // 5. 정예 멤버 30명이 좋아하는 작품 중 내가 안 본 것 추천
        List<Long> recommendedIds = animeRatingRepository.findRecommendedAnimeIds(
                similarUserIds,
                new ArrayList<>(myWatchedIds),
                PageRequest.of(0, 10)
        );
        System.out.println("STEP 5: 최종 추천 애니메이션 ID 수 = " + recommendedIds.size());

        if (recommendedIds.isEmpty()) {
            System.out.println("RESULT: 후보 유저들의 평점이 낮거나(4점 미만), 내가 이미 다 본 작품들뿐입니다.");
            return Collections.emptyList();
        }

        // 6. ID 리스트를 실제 Anime 엔티티 리스트로 변환
        List<Anime> unsortedAnimes = animeRepository.findAllByMalIdIn(recommendedIds);

        System.out.println("=== 추천 로직 완료 (최종 반환 개수: " + unsortedAnimes.size() + ") ===");

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