package com.example.demo.util; // 본인의 패키지 경로에 맞게 수정하세요

import com.example.demo.user.entity.AnimeRating;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.AnimeRatingRepository;
import com.example.demo.user.repository.AnimeRepository;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RatingDataInitializer implements CommandLineRunner {

    private final AnimeRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. 이미 데이터가 있다면 중복 실행 방지
        if (ratingRepository.count() > 100) {
            System.out.println("이미 평점 데이터가 존재하여 초기화를 건너뜁니다.");
            return;
        }

        // 2. 현재 DB에 있는 애니메이션의 malId 세트 가져오기 (매핑 확인용)
        Set<Long> existingMalIds = animeRepository.findAll().stream()
                .map(a -> a.getMalId().longValue())
                .collect(Collectors.toSet());

        // 3. resources 폴더의 CSV 파일 읽기
        InputStream is = new ClassPathResource("rating_sample.csv").getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        reader.readLine(); // 첫 줄(헤더: user_id, anime_id, rating) 건너뛰기

        int count = 0;
        int maxCount = 1000; // 초기 테스트를 위해 1000개로 제한 (필요시 조절)

        System.out.println("데이터 삽입 시작...");

        while ((line = reader.readLine()) != null && count < maxCount) {
            String[] cols = line.split(",");
            if (cols.length < 3) continue;

            Long csvUserId = Long.parseLong(cols[0].trim());
            Long csvMalId = Long.parseLong(cols[1].trim());
            int csvRating = Integer.parseInt(cols[2].trim());

            // 4. 필터링: 내가 가진 애니메이션 정보가 있고, 점수가 -1(무응답)이 아닌 경우만
            if (existingMalIds.contains(csvMalId) && csvRating != -1) {

                // 5. Kaggle ID로 유저 찾기 (없으면 새로 생성)
                User user = userRepository.findByKaggleId(csvUserId).orElseGet(() -> {
                    User newUser = User.builder()
                            .username("KaggleUser_" + csvUserId)
                            .password("dummy_password_1234") // 가짜 비밀번호
                            .kaggleId(csvUserId)             // 매핑 필드에 Kaggle ID 저장
                            .age(20)
                            .gender("Unknown")
                            .firstLogin(false)               // 테스트 유저는 온보딩 완료 처리
                            .build();
                    return userRepository.save(newUser);
                });

                // 6. 평점 정보 저장
                AnimeRating rating = new AnimeRating();
                rating.setUser(user);
                rating.setMalId(csvMalId);
                rating.setScore(csvRating);
                ratingRepository.save(rating);

                count++;
            }
        }
        System.out.println(">>> 총 " + count + "개의 Kaggle 기반 평점 데이터 삽입 완료!");
    }
}