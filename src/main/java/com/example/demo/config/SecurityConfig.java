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
            // 인증 없이 허용
            .requestMatchers(
                "/api/user/register",
                "/login",
                "/logout"
            ).permitAll()

            // API는 무조건 로그인 필요
            .requestMatchers("/api/**").authenticated()

            // 나머지는 전부 허용 (React 라우팅 때문)
            .anyRequest().permitAll()
        )

        .formLogin(form -> form
            .loginProcessingUrl("/login")
            .successHandler((req, res, auth) -> res.setStatus(200))
            .failureHandler((req, res, e) -> res.setStatus(401))
        )

        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
        );

    return http.build();
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}