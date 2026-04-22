package com.example.demo.config;

import com.example.demo.user.entity.User;
import com.example.demo.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
                // 1. CORS 설정 (CorsConfig.java와 연동되거나 여기서 직접 처리)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // 2. 세션 정책 설정: 필요 시 세션을 생성하도록 명시
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // 3. 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/register", "/login").permitAll()
                        .requestMatchers("/api/anime/**").permitAll()
                        .requestMatchers("/api/recommendations/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )

                // 4. 로그인 설정 (SameSite=None 설정 포함)
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .successHandler((req, res, auth) -> {
                            res.setContentType("application/json;charset=UTF-8");

                            // 🔥 핵심: 크로스 도메인 간 세션 공유를 위한 쿠키 설정
                            // Render(HTTPS)에서 로컬(HTTP)로 쿠키를 보낼 때 SameSite=None; Secure가 필수입니다.
                            String cookieHeader = String.format("JSESSIONID=%s; Path=/; HttpOnly; SameSite=None; Secure",
                                    req.getSession().getId());
                            res.setHeader("Set-Cookie", cookieHeader);

                            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                            User user = userDetails.getUser();

                            String json = String.format(
                                    "{\"username\":\"%s\", \"firstLogin\":%s}",
                                    user.getUsername(),
                                    user.isFirstLogin()
                            );
                            res.getWriter().write(json);
                            res.getWriter().flush();
                        })
                        .failureHandler((req, res, e) -> {
                            res.setStatus(401);
                        })
                )

                // 5. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://ani-frontend-ek9a.onrender.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}