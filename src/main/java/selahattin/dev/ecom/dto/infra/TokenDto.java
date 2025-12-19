package selahattin.dev.ecom.dto.infra;

import java.util.List;

import javax.crypto.SecretKey;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TokenDto {
    String subject;
    List<String> roles;
    String uid;
    String firstName;
    String lastName;
    String phoneNumber;
    long expirationMillis;
    SecretKey key;
}
