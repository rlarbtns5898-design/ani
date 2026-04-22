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
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AnimeRatingRepository ratingRepository;
    private final AnimeRepository animeRepository;

    public List<Anime> getRecommendations(User user) {
        // 1. 내가 본 애니메이션 정보
        List<AnimeRating> myRatings = ratingRepository.findByUser(user);
        List<Long> myWatchedIds = myRatings.stream().map(AnimeRating::getMalId).toList();

        if (myWatchedIds.isEmpty()) {
            return animeRepository.findTop10ByOrderByScoreDesc();
        }

        // 2. 유사 유저 50명 찾기 (폭을 넓혀야 의외성이 생깁니다)
        List<Long> similarUserIds = ratingRepository.findSimilarUserIds(
                user.getId(), PageRequest.of(0, 50));

        if (!similarUserIds.isEmpty()) {
            // 3. 추천 후보군 40개 추출
            List<Long> recommendedIds = ratingRepository.findRecommendedAnimeIds(
                    similarUserIds, myWatchedIds, PageRequest.of(0, 40));

            if (!recommendedIds.isEmpty()) {
                List<Anime> candidates = animeRepository.findAllByMalIdIn(recommendedIds);

                // 🌟 핵심: 가져온 40개를 무작위로 섞습니다.
                // 이렇게 하면 매번 '진격의 거인'만 나오던 정형화된 리스트가 바뀝니다.
                List<Anime> mutableCandidates = new ArrayList<>(candidates);
                Collections.shuffle(mutableCandidates);

                // 4. 섞인 리스트에서 상위 10개만 최종 반환
                return mutableCandidates.stream().limit(10).toList();
            }
        }

        // 5. Fallback: 데이터가 너무 적을 경우 평점 순 추천
        return animeRepository.findTop10ByOrderByScoreDesc();
    }
}