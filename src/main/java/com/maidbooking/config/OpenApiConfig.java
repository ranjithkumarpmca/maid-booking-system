package com.maidbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI maidBookingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Maid Booking System API")
                .version("1.0.0")
                .description("Discover maids, book them (instant, scheduled, recurring), pay and cancel."));
    }
}
