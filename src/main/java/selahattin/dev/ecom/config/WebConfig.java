package selahattin.dev.ecom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import selahattin.dev.ecom.utils.enums.UserStatus;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "/public/" ile gelen istekleri -> fiziksel "public" klasörüne yönlendir
        registry.addResourceHandler("/assets/public/products/**")
                .addResourceLocations("file:assets/public/products/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, UserStatus>() {
            @Override
            public UserStatus convert(String source) {
                return UserStatus.fromValue(source);
            }
        });
    }
}