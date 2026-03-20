package com.example.demo.user.service;

import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void register(String username, String password, Integer age, String gender) {

        String encodedPassword = passwordEncoder.encode(password); // ⭐ 핵심

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword); // ⭐ 암호화 저장
        user.setAge(age);
        user.setGender(gender);

        userRepository.save(user);
    }
}