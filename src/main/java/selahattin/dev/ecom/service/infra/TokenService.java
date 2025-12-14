package selahattin.dev.ecom.service.infra;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.properties.JwtProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenStoreService tokenStoreService;
    private final JwtProperties jwtProperties;

    public void storeToken(String email, CookieDto dto) {
        tokenStoreService.storeToken(
                email,
                dto.getDeviceId(),
                dto.getRefreshToken(),
                jwtProperties.getRefreshTokenExpirationMs());
    }

    public void deleteToken(String email, String deviceId) {
        tokenStoreService.deleteToken(email, deviceId);
    }

    public boolean validateToken(String email, String deviceId, String rawToken) {
        return tokenStoreService.validateToken(email, deviceId, rawToken);
    }
}