package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
<<<<<<< HEAD

                        .allowedOrigins("https://ani-frontend-ek9a.onrender.com",
                                "http://localhost:5173") // React 주소,testing용 로컬
=======
                        .allowedOrigins("http://localhost:5173","https://ani-frontend-ek9a.onrender.com") // React 주소
>>>>>>> f23ee92e5c98d4bf95e111010c6299e56d4ee4a4
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}