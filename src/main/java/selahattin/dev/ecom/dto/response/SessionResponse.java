package selahattin.dev.ecom.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {
    private String deviceId; // Silmek için lazım
    private String ipAddress; // Tanımak için lazım
    private String userAgent; // Tanımak için lazım (Chrome on Windows vb.)
    private long lastActiveAt; // Ne zaman aktifti?
    private boolean isCurrent; // Şu anki oturumu mu?
}