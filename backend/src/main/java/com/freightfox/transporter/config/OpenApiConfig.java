package com.freightfox.transporter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transporterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FreightFox Transporter Assignment API")
                .version("v1")
                .description("Submit lanes and transporter quotes, then compute the optimal transporter-to-lane "
                        + "assignment under a maximum transporter limit."));
    }
}
