package selahattin.dev.ecom.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.properties.ClientProperties;
import selahattin.dev.ecom.config.properties.JwtProperties;
import selahattin.dev.ecom.security.jwt.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final ClientProperties clientProperties;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoint'ler
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/auth/public/**").permitAll()

                        // Webhook'lar (Dış dünyadan JWT'siz gelecek)
                        .requestMatchers("/api/v1/webhooks/**").permitAll()

                        // Statik Dosyalar ve Dokümantasyon
                        .requestMatchers("/assets/public/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Kalan tüm endpointler için yetki zorunlu
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 1. STANDART API CORS AYARLARI (Sadece senin Frontend'ine izin verir)
        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOrigins(clientProperties.getCorsAllowedOrigins());
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("*"));
        apiConfig.setAllowCredentials(true);

        // 2. WEBHOOK CORS AYARLARI (Iyzico, Stripe gibi yerlerden gelen POST'lara açık)
        CorsConfiguration webhookConfig = new CorsConfiguration();
        webhookConfig.setAllowedOrigins(List.of("*"));
        webhookConfig.setAllowedMethods(List.of("POST", "OPTIONS"));
        webhookConfig.setAllowedHeaders(List.of("*"));
        webhookConfig.setAllowCredentials(false); // Wildcard origin kullanıldığı için false olmak zorunda

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Spring önce spesifik olanı kontrol eder. Webhook yollarına özel CORS:
        source.registerCorsConfiguration("/api/v1/webhooks/**", webhookConfig);
        // Geri kalan tüm API yollarına standart CORS:
        source.registerCorsConfiguration("/**", apiConfig);

        return source;
    }
}