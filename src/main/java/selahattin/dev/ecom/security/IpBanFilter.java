package selahattin.dev.ecom.security;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.infra.IpBanService;

@Component
@Order(1)
@RequiredArgsConstructor
public class IpBanFilter extends OncePerRequestFilter {

    private final IpBanService ipBanService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        if (ipBanService.isBanned(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            ApiResponse.error("Erişiminiz geçici olarak engellendi.", 4030)));
            return;
        }

        filterChain.doFilter(request, response);

        // Tüm 4xx yanıtları (401 hariç) ban sayacına ekle — Spring Security dahil
        int status = response.getStatus();
        if (status >= 400 && status != 401) {
            ipBanService.recordBadRequest(ip);
        }
    }
}
