package com.example.demo.config;

import com.example.demo.user.entity.User;
import com.example.demo.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS 설정 연결
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // 2. 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/register", "/login").permitAll()
                        .requestMatchers("/api/anime/**").permitAll()
                        .requestMatchers("/api/recommendations/**").authenticated() // 추천 경로는 인증 필수
                        .requestMatchers("/api/**").authenticated() // 기타 API도 인증 필요
                        .anyRequest().permitAll()
                )

                // 3. 로그인 설정
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            res.setContentType("application/json;charset=UTF-8");
                            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                            User user = userDetails.getUser();

                            String json = String.format(
                                    "{\"username\":\"%s\", \"firstLogin\":%s}",
                                    user.getUsername(),
                                    user.isFirstLogin()
                            );
                            res.getWriter().write(json);
                        })
                        .failureHandler((req, res, e) -> {
                            res.setStatus(401);
                        })
                )

                // 4. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                        .deleteCookies("JSESSIONID") // 로그아웃 시 세션 쿠키 확실히 삭제
                        .invalidateHttpSession(true)
                );

        return http.build();
    }

    // 5. CORS 세부 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🔥 허용할 프론트엔드 주소 (포트 5173 포함)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://ani-frontend-ek9a.onrender.com"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        // 🌟 세션 쿠키 전달을 위한 핵심 설정
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}