package com.example.demo.user.service;
import com.example.demo.user.entity.Anime;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.demo.user.entity.AnimeRating;
import java.util.List;
import org.springframework.data.domain.PageRequest;
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AnimeRatingRepository ratingRepository;
    private final AnimeRepository animeRepository;

    public List<Anime> getRecommendations(User user) {
        // 1. 내가 평가한 애니메이션 ID 리스트
        List<Long> myWatchedIds = ratingRepository.findByUser(user).stream()
                .map(AnimeRating::getMalId)
                .toList();

        if (myWatchedIds.isEmpty()) {
            // 본 게 없다면 전체 평점 순 추천
            return animeRepository.findTop10ByOrderByScoreDesc();
        }

        // 2. 유사 유저 찾기 (상위 10명)
        List<Long> similarUserIds = ratingRepository.findSimilarUserIds(
                myWatchedIds, user.getId(), PageRequest.of(0, 10));

        if (!similarUserIds.isEmpty()) {
            // 3. 유사 유저 기반 추천 (상위 10개)
            List<Long> recommendedIds = ratingRepository.findRecommendedAnimeIds(
                    similarUserIds, myWatchedIds, PageRequest.of(0, 10));

            if (!recommendedIds.isEmpty()) {
                return animeRepository.findAllByMalIdIn(recommendedIds);
            }
        }

        // 4. [Fallback] 유사 유저가 없으면 내가 좋아한 장르 기반 추천
        return getGenreBasedRecommendations(user, myWatchedIds);
    }

    private List<Anime> getGenreBasedRecommendations(User user, List<Long> myWatchedIds) {
        // 내가 4점 이상 준 애니들의 장르 분석 (이전 단계에서 만든 로직 활용)
        // ... (생략) ...
        return animeRepository.findTop10ByOrderByScoreDesc(); // 임시 결과
    }
}