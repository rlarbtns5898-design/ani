package com.example.demo.user.service;

import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void register(String username, String password, Integer age, String gender) {

        // 1. 중복 체크 (이게 바로 그 '문지기' 로직입니다)
        if (userRepository.existsByUsername(username)) {
            // 이미 아이디가 있다면 예외를 던져서 이후 로직(암호화, 저장)이 실행되지 않게 막습니다.
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 3. 유저 엔티티 생성 및 설정
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setAge(age);
        user.setGender(gender);

        // 중요: 회원가입 시 온보딩을 위해 true로 설정 (엔티티 기본값이 true라면 생략 가능)
        user.setFirstLogin(true);

        // 4. DB 저장
        userRepository.save(user);
    }
}