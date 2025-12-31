package selahattin.dev.ecom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "/public/" ile gelen istekleri -> fiziksel "public" klasörüne yönlendir
        registry.addResourceHandler("/assets/public/products/**")
                .addResourceLocations("file:assets/public/products/");
    }
}