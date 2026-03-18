package com.example.demo.config;

import com.example.demo.user.security.CustomLoginSuccessHandler;
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
    private final CustomLoginSuccessHandler customLoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/css/**").permitAll()
                        .requestMatchers("/onboarding","/onboarding/save").authenticated()
                        .requestMatchers("/board/write", "/board/delete/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")                                      .loginProcessingUrl("/login")
                        .failureUrl("/login?error=true")
                        .successHandler(customLoginSuccessHandler)                            .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")

                );


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}