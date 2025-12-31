package selahattin.dev.ecom.utils.cookie;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.properties.JwtProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;

@Component
@RequiredArgsConstructor
public class CookieFactory {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public CookieDto create(CustomUserDetails user) {
        String newDeviceId = UUID.randomUUID().toString();
        return createDto(user, newDeviceId);
    }

    public CookieDto create(CustomUserDetails user, String existingDeviceId) {
        return createDto(user, existingDeviceId);
    }

    private CookieDto createDto(CustomUserDetails user, String deviceId) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUser().getEmail(),
                user.getUser().getId().toString(),
                user.getUser().getRoles().stream().map(r -> r.getName()).toList(),
                deviceId);

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), deviceId);

        return CookieDto.builder()
                .deviceId(deviceId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry((int) (jwtProperties.getAccessTokenExpirationMs() / 1000))
                .refreshTokenExpiry((int) (jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .build();
    }
}