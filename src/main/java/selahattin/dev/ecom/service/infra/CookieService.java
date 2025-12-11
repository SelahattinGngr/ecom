package selahattin.dev.ecom.service.infra;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.infra.CookieDto;
import selahattin.dev.ecom.utils.cookie.CookieUtil;
import selahattin.dev.ecom.utils.enums.CookieConstants;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final CookieUtil cookieUtil;

    public void createCookies(CookieDto cookieDto, HttpServletResponse response) {
        cookieUtil.addCookie(
                response,
                CookieConstants.ACCESS_TOKEN.getCookieName(),
                cookieDto.getAccessToken(),
                cookieDto.getAccessTokenExpiry());
        cookieUtil.addCookie(response,
                CookieConstants.REFRESH_TOKEN.getCookieName(),
                cookieDto.getRefreshToken(),
                cookieDto.getRefreshTokenExpiry());

        cookieUtil.addCookie(response,
                CookieConstants.DEVICE_ID.getCookieName(),
                cookieDto.getDeviceId(),
                cookieDto.getRefreshTokenExpiry());
    }

    public void refreshCookies(CookieDto cookieDto, HttpServletResponse response) {
        cookieUtil.addCookie(
                response,
                CookieConstants.ACCESS_TOKEN.getCookieName(),
                cookieDto.getAccessToken(),
                cookieDto.getAccessTokenExpiry());
        cookieUtil.addCookie(response,
                CookieConstants.REFRESH_TOKEN.getCookieName(),
                cookieDto.getRefreshToken(),
                cookieDto.getRefreshTokenExpiry());
    }

    public void clearCookies(HttpServletResponse response) {
        cookieUtil.clearCookie(response, CookieConstants.ACCESS_TOKEN.getCookieName());
        cookieUtil.clearCookie(response, CookieConstants.REFRESH_TOKEN.getCookieName());
        cookieUtil.clearCookie(response, CookieConstants.DEVICE_ID.getCookieName());
    }

}