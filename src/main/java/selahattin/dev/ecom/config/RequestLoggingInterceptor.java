package selahattin.dev.ecom.config;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.infra.ActivityLogDto;
import selahattin.dev.ecom.security.CustomUserDetails;
import selahattin.dev.ecom.service.infra.RedisQueueService;

@Component
@RequiredArgsConstructor
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private final RedisQueueService redisQueueService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {

        String endpoint = request.getRequestURI();

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

        redisQueueService.enqueueActivityLog(ActivityLogDto.builder()
                .userId(userId)
                .deviceId(deviceId)
                .ipAddress(ip)
                .method(request.getMethod())
                .endpoint(endpoint)
                .statusCode(response.getStatus())
                .timestamp(OffsetDateTime.now())
                .build());
    }
}
