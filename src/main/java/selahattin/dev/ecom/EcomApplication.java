package selahattin.dev.ecom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import selahattin.dev.ecom.dev.CyberPsychosisBean;

@SpringBootApplication
public class EcomApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomApplication.class, args);
	}

	@Bean
	@Profile("test")
	public CyberPsychosisBean cyberPsychosisBean() {
		return new CyberPsychosisBean();
	}
}
