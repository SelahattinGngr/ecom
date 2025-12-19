package selahattin.dev.ecom.security.jwt;

import java.io.IOException;
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
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.CookieService;

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

                // Token'dan tüm bilgileri sömürüyoruz
                String email = jwtTokenProvider.extractUsername(jwt, true);
                List<String> roleNames = jwtTokenProvider.extractRoles(jwt);
                String userId = jwtTokenProvider.extractUserId(jwt);
                String firstName = jwtTokenProvider.extractFirstName(jwt);
                String lastName = jwtTokenProvider.extractLastName(jwt);
                String phoneNumber = jwtTokenProvider.extractPhoneNumber(jwt);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // String Listesi -> RoleEntity Setine dönüşüm
                    Set<RoleEntity> roleEntities = roleNames.stream()
                            .map(roleName -> RoleEntity.builder().name(roleName).build())
                            .collect(Collectors.toSet());

                    // User nesnesini DB'ye sormadan, Token verileriyle dolduruyoruz
                    UserEntity partialUser = UserEntity.builder()
                            .id(UUID.fromString(userId))
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .phoneNumber(phoneNumber)
                            .roles(roleEntities)
                            .build();

                    // Password olmadığı için sadece user entity veriyorum
                    CustomUserDetails userDetails = new CustomUserDetails(partialUser);

                    // Spring Security'e yetki veriyorum. Credentials (2. parametre) null çünkü
                    // password yok.
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Authentication Filter Error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}