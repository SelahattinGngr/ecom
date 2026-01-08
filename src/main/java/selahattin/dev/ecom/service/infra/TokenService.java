package selahattin.dev.ecom.service.infra;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.properties.JwtProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.dto.infra.SessionPayload;
import selahattin.dev.ecom.dto.response.SessionResponse;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenStoreService tokenStoreService;
    private final JwtProperties jwtProperties;

    public void storeSession(UserEntity user, CookieDto dto, String ipAddress, String userAgent) {

        // Rol isimlerini çek
        List<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();

        SessionPayload payload = SessionPayload.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .roleNames(roleNames)
                .deviceId(dto.getDeviceId())
                .hashedRefreshToken(dto.getRefreshToken())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .lastActiveAt(System.currentTimeMillis())
                .build();

        tokenStoreService.storeSession(
                user.getEmail(),
                dto.getDeviceId(),
                payload,
                jwtProperties.getRefreshTokenExpirationMs());
    }

    // Filter'ın kullanacağı metod
    public SessionPayload getSession(String email, String deviceId) {
        return tokenStoreService.getSession(email, deviceId);
    }

    public List<SessionResponse> getUserSessions(String email, String currentDeviceId) {
        return tokenStoreService.getUserSessions(email, currentDeviceId);
    }

    public void deleteToken(String email, String deviceId) {
        tokenStoreService.deleteToken(email, deviceId);
    }

    public void deleteAllTokens(String email) {
        tokenStoreService.deleteAllTokensForUser(email);
    }

    public boolean validateToken(String email, String deviceId, String rawToken) {
        // Burayı TokenStoreService içinde payload.getHashedRefreshToken() ile check
        // edecek şekilde güncellemelisin
        return tokenStoreService.validateToken(email, deviceId, rawToken);
    }
}