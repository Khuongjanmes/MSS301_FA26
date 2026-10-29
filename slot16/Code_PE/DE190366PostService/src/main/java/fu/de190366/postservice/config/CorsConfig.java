package fu.de190366.postservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS cho service MVC (servlet). Cho phép mọi origin.
 * Cần thiết để Swagger UI của MediaGateway (origin :8080) có thể fetch trực tiếp
 * /v3/api-docs của service này (cross-origin) khi gom tài liệu.
 * Dùng CorsFilter (servlet filter) để áp dụng cho MỌI request, kể cả /v3/api-docs.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");   // mọi origin
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(false);     // phải false khi origin pattern là "*"

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
