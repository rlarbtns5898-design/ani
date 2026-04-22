import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "https://ani-frontend-ek9a.onrender.com")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true); // 세션 유지를 위해 필수
    }
}