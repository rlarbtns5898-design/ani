package com.example.demo.user.service;

import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(String username, String password, Integer age, String gender) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .age(age)
                .gender(gender)
                .firstLogin(true)
                .build();

        userRepository.save(user);
    }
}