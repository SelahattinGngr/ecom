package selahattin.dev.ecom.utils.cookie;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.JwtProperties;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;

@Component
@RequiredArgsConstructor
public class CookieFactory {

    private final JwtTokenProvider jwtService;
    private final JwtProperties jwtDto;

    public CookieDto create(CustomUserDetails user) {
        return new CookieDto(user, jwtService, jwtDto);
    }
}
