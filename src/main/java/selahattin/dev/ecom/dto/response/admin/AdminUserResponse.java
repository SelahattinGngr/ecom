package selahattin.dev.ecom.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private List<String> roles;
    private OffsetDateTime createdAt;
}