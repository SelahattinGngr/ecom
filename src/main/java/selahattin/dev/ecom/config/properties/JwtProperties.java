package selahattin.dev.ecom.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Validated
@ConfigurationProperties(prefix = "selahattin.dev.jwt")
public class JwtProperties {

    @NotBlank(message = "Access Secret Key boş olamaz!")
    private String accessSecretKey;

    @NotBlank(message = "Refresh Secret Key boş olamaz!")
    private String refreshSecretKey;

    @Positive(message = "Access Token süresi 0'dan büyük olmalı")
    private long accessTokenExpirationMs;

    @Positive(message = "Refresh Token süresi 0'dan büyük olmalı")
    private long refreshTokenExpirationMs;
}