package selahattin.dev.ecom.service.infra;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.JwtProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenStoreService tokenStoreService;
    private final JwtProperties jwtDto;

    public void storeToken(String email, CookieDto dto) {
        tokenStoreService.storeToken(email, dto.getDeviceId(), dto.getRefreshToken(),
                jwtDto.getRefreshTokenExpirationMs());
    }

    public void deleteToken(String email, String deviceId) {
        tokenStoreService.deleteToken(email, deviceId);
    }

    public String getToken(String email, String deviceId) {
        return tokenStoreService.getToken(email, deviceId);
    }
}
