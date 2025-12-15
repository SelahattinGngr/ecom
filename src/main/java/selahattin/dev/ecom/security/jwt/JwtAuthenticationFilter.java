package selahattin.dev.ecom.security.jwt;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.UserEntity;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.CookieService;
import selahattin.dev.ecom.utils.enums.Role;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = cookieService.extractAccessToken(request);
            if (jwt != null && jwtTokenProvider.validateAccessToken(jwt)) {

                String email = jwtTokenProvider.extractUsername(jwt, true);
                String roleString = jwtTokenProvider.extractRole(jwt);
                String userId = jwtTokenProvider.extractUserId(jwt);
                String firstName = jwtTokenProvider.extractFirstName(jwt);
                String lastName = jwtTokenProvider.extractLastName(jwt);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserEntity partialUser = UserEntity.builder()
                            .id(UUID.fromString(userId))
                            .email(email)
                            .role(Role.valueOf(roleString))
                            .firstName(firstName)
                            .lastName(lastName)
                            .build();

                    CustomUserDetails userDetails = new CustomUserDetails(partialUser);

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleString);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            Collections.singleton(authority));

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token bozuksa, süresi dolmuşsa veya parse hatası varsa logla ve geç.
            // Context boş kalacağı için kullanıcı 401/403 alır.
            log.error("Authentication Filter Error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}