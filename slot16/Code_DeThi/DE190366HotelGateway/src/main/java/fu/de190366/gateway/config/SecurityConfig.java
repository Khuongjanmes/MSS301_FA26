package fu.de190366.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security for the reactive gateway (WebFlux -> SecurityWebFilterChain, not SecurityFilterChain).
 * The exam defines no authentication, so every request is permitted; without this bean the
 * default config would demand HTTP Basic login and block all API calls through the gateway.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(Customizer.withDefaults())                    // uses CorsConfig.corsConfigurationSource
                .csrf(ServerHttpSecurity.CsrfSpec::disable)         // stateless REST API, no browser session
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
}
