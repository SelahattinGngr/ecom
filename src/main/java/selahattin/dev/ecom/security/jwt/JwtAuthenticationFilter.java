package selahattin.dev.ecom.security.jwt;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.exception.ResourceNotFoundException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String path = request.getRequestURI();
            String method = request.getMethod();

            if (shouldSkipFiltering(path, method)) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = extractTokenFromCookie(request);
            if (jwt != null && jwtTokenProvider.validateAccessToken(jwt)) {
                String userEmail = jwtTokenProvider.extractUsername(jwt, true);

                if (userEmail == null) {
                    throw new ResourceNotFoundException("JWT'den kullanıcı adı alınamadı.");
                }

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token süresi dolmuş veya bozuk olabilir, logla ve devam et (403 alacak)
            log.error("Authentication Error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Cookie dizisini tarar ve accessToken'ı bulur.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> ACCESS_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Belirli yollar için filtrelemeyi atlar.
     */
    private boolean shouldSkipFiltering(String path, String method) {
        return path.startsWith("/api/v1/auth/public/");
    }
}