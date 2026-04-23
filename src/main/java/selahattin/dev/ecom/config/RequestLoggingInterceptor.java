package selahattin.dev.ecom.config;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.IpBanService;
import selahattin.dev.ecom.service.infra.UserActivityEventService;

@Component
@RequiredArgsConstructor
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private final UserActivityEventService userActivityEventService;
    private final IpBanService ipBanService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {

        String endpoint = request.getRequestURI();

        // Gürültülü path'leri atla
        if (endpoint.startsWith("/actuator")
                || endpoint.startsWith("/assets")
                || endpoint.startsWith("/.well-known")) {
            return;
        }

        UUID userId = null;
        String deviceId = (String) request.getAttribute("deviceId");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            userId = details.getUser().getId();
        }

        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        int status = response.getStatus();
        userActivityEventService.log(userId, deviceId, ip, request.getMethod(), endpoint, status);

        // 4xx hataları (401 hariç — token expire normal akış) ban sayacına ekle
        if (status >= 400 && status != 401) {
            ipBanService.recordBadRequest(ip);
        }
    }
}
