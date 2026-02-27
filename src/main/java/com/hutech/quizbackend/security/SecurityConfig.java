package com.hutech.quizbackend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Chống DDoS cơ bản bằng cách giới hạn truy cập và CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. Tắt CSRF tạm thời để phát triển (Sẽ cấu hình lại khi deploy)
                .csrf(csrf -> csrf.disable())
                // 3. Chống SQL Injection: Spring Security + JPA tự động bảo vệ qua Parameterized Queries
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login**", "/error**", "/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 4. Cấu hình Google Login và xử lý lưu Database
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler) // Gọi trình xử lý để lưu User vào MySQL
                );

        return http.build();
    }

    // Cấu hình CORS cho phép Frontend (Buổi 4) kết nối
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}