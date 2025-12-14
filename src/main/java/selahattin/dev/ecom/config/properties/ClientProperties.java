package selahattin.dev.ecom.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "selahattin.dev.client")
public class ClientProperties {

	@NotBlank(message = "Frontend URL (selahattin.dev.client.frontend-url) tanımlanmalıdır! "
			+ "\n(http://localhost:3000) formatında olmalıdır.")
	private String frontendUrl;

	@NotBlank(message = "Email Verification Path (selahattin.dev.client.email-verification-path) tanımlanmalıdır! "
			+ "\n(/path?token=) formatında olmalıdır.")
	private String emailVerificationPath;

	@NotEmpty(message = "En az bir tane CORS origin tanımlanmalıdır!")
	private List<String> corsAllowedOrigins;
}
