package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.user.security.CustomUserDetails;
import com.example.demo.user.entity.User;

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
                .requestMatchers("/api/anime/**").permitAll()
                .requestMatchers("/api/**").authenticated() // 🔥 API는 로그인 필요
                .anyRequest().permitAll()
            )

            .formLogin(form -> form
                .loginProcessingUrl("/login") // 🔥 React에서 POST /login
                .usernameParameter("username") // 기본값이라 생략 가능
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