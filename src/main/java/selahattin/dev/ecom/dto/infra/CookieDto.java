package selahattin.dev.ecom.dto.infra;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;
import selahattin.dev.ecom.config.JwtProperties;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;

@Data
public class CookieDto {

    private String deviceId;
    private String accessToken;
    private String refreshToken;
    private int accessTokenExpiry;
    private int refreshTokenExpiry;

    public CookieDto(UserDetails user, JwtTokenProvider jwtService, JwtProperties jwtProperties) {
        this.deviceId = UUID.randomUUID().toString();
        this.accessToken = jwtService.generateAccessToken(user);
        this.refreshToken = jwtService.generateRefreshToken(user);
        this.accessTokenExpiry = (int) (jwtProperties.getAccessTokenExpirationMs() / 1000);
        this.refreshTokenExpiry = (int) (jwtProperties.getRefreshTokenExpirationMs() / 1000);
    }
}