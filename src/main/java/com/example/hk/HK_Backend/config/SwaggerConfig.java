package com.example.hk.HK_Backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI hkPgOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development"),
                        new Server().url("https://your-backend.railway.app").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Obtain a JWT token from **POST /api/auth/login**, " +
                                                "then paste it here. The 'Bearer ' prefix is added automatically."
                                        )
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("HK PG — Boys Accommodation API")
                .version("1.0.0")
                .description(
                        "## HK PG Backend REST API\n\n" +
                        "Production REST API for **HK PG Boys Accommodation**, Akurdi, Pune.\n\n" +
                        "### How to authenticate\n" +
                        "1. Register a new account via `POST /api/auth/register`\n" +
                        "2. Or login via `POST /api/auth/login`\n" +
                        "3. Copy the `token` from the response\n" +
                        "4. Click **Authorize 🔒** above and paste the token\n\n" +
                        "### Roles\n" +
                        "- **STUDENT** — can submit applications, view own applications\n" +
                        "- **ADMIN** — full access including dashboard and application management"
                )
                .contact(new Contact()
                        .name("HK PG")
                        .email("admin@hkpg.com")
                        .url("https://hkpg.vercel.app")
                )
                .license(new License()
                        .name("Private — All Rights Reserved")
                        .url("https://hkpg.vercel.app")
                );
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("HK PG Website")
                .url("https://hkpg.vercel.app");
    }
}
