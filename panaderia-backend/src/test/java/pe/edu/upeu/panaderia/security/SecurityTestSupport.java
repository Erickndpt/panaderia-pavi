package pe.edu.upeu.panaderia.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@TestConfiguration
public class SecurityTestSupport {

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            throw new JwtException("JwtDecoder de prueba: los tests no validan tokens reales de Keycloak");
        };
    }

    @Bean
    MockMvcBuilderCustomizer authenticatedUserMockMvcCustomizer() {
        return builder -> builder.defaultRequest(get("/").with(jwt().authorities(allRoles())));
    }

    private static List<GrantedAuthority> allRoles() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_VENDEDOR"),
                new SimpleGrantedAuthority("ROLE_SUPERVISOR"),
                new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
