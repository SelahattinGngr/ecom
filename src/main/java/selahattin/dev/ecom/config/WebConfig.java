package selahattin.dev.ecom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.ProductStatus;
import selahattin.dev.ecom.utils.enums.UserStatus;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor);
    }

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
        registry.addConverter(new Converter<String, ProductStatus>() {
            @Override
            public ProductStatus convert(String source) {
                return ProductStatus.fromValue(source);
            }
        });
        registry.addConverter(new Converter<String, OrderStatus>() {
            @Override
            public OrderStatus convert(String source) {
                return OrderStatus.fromValue(source);
            }
        });
    }
}