import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().and() // 다시 원래대로!
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/register", "/login").permitAll()
                        .requestMatchers("/api/anime/**").permitAll()
                        .requestMatchers("/api/**").authenticated() // 추천 경로도 여기에 포함됨
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                                .loginProcessingUrl("/login")
                        // ... 기존에 잘 되던 successHandler/failureHandler 코드 그대로 사용
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}