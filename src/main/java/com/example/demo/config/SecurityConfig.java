package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            
    .cors()
    .and()
    .csrf().disable()
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/user/register", "/login").permitAll()
        .anyRequest().authenticated()
    )
    .formLogin(form -> form
        .loginProcessingUrl("/login")
        .successHandler((request, response, authentication) -> {
            response.setStatus(200); // ⭐ redirect 막기
        })
        .failureHandler((request, response, exception) -> {
            response.setStatus(401); // ⭐ 실패 시 상태코드
        })
    )
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
    );    // 로그아웃도 비활성화

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}