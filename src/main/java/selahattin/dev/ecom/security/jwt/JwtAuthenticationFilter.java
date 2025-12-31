package selahattin.dev.ecom.security.jwt;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import selahattin.dev.ecom.dto.infra.SessionPayload;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.CookieService;
import selahattin.dev.ecom.service.infra.RoleCacheService;
import selahattin.dev.ecom.service.infra.TokenService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;
    private final RoleCacheService roleCacheService;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = cookieService.extractAccessToken(request);

            if (jwt != null && jwtTokenProvider.validateAccessToken(jwt)) {

                String email = jwtTokenProvider.extractUsername(jwt, true);
                String deviceId = jwtTokenProvider.extractDeviceId(jwt, true);

                if (email != null && deviceId != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // KULLANICI DETAYLARINI REDIS'TEN ÇEK (TokenService üzerinden)
                    SessionPayload session = tokenService.getSession(email, deviceId);

                    if (session != null) {
                        // Yetkileri belirle
                        Set<RoleEntity> roleEntities = session.getRoleNames().stream()
                                .map(roleName -> {
                                    List<String> permissions = roleCacheService.getPermissionsForRole(roleName);
                                    Set<PermissionEntity> perms = (permissions == null) ? Collections.emptySet()
                                            : permissions.stream().map(p -> PermissionEntity.builder().name(p).build())
                                                    .collect(Collectors.toSet());

                                    return RoleEntity.builder().name(roleName).permissions(perms).build();
                                })
                                .collect(Collectors.toSet());

                        // UserEntity Oluştur (Redis verisiyle)
                        UserEntity partialUser = UserEntity.builder()
                                .id(UUID.fromString(session.getUserId()))
                                .email(session.getEmail())
                                .firstName(session.getFirstName())
                                .lastName(session.getLastName())
                                .phoneNumber(session.getPhoneNumber())
                                .roles(roleEntities)
                                .build();

                        CustomUserDetails userDetails = new CustomUserDetails(partialUser);

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        // Token geçerli ama Redis oturumu silinmiş (Örn: Admin banlamış)
                        log.warn("Redis oturumu bulunamadı: {}", email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Auth Filter Hatası: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}