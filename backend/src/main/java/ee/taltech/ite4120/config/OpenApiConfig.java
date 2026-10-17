package ee.taltech.ite4120.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tells the generated API contract what {@link SecurityConfig} enforces: every
 * {@code /api/**} endpoint wants {@code Authorization: Bearer <username>}.
 *
 * <p>Without this, Swagger UI happily generates header-less curl commands whose
 * only answer is a 401 — the single most reported confusion in the template.
 * With it, every operation carries a padlock, the <em>Authorize</em> button
 * accepts the username, and "Try it out" sends the header.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI apiDescription() {
        return new OpenAPI()
                .info(new Info()
                        .title("Animals Register — ITE4120 course template")
                        .description("""
                                Every /api endpoint requires `Authorization: Bearer <username>`. \
                                In local mock mode any username works — click Authorize and type one.""")
                        .version("dev"))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Any username in local mock mode — e.g. plain `igor`.")))
                // Global: every operation inherits the requirement, matching the
                // security chain's `/api/**` rule.
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
