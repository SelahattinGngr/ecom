package selahattin.dev.ecom.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CurrentUserResponse {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String role;
    // İleride buraya avatarUrl, phone number vs. eklenir.
}
