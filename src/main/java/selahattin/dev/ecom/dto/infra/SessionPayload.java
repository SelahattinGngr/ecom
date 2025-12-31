package selahattin.dev.ecom.dto.infra;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionPayload {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<String> roleNames;

    private String deviceId;
    private String hashedRefreshToken;
    private String ipAddress;
    private String userAgent;
    private Long lastActiveAt;
}