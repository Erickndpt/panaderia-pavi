package pe.edu.upeu.panaderia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${app.security.permit-all:false}") boolean permitAll) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/actuator/health",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**").permitAll();

                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    if (permitAll) {
                        auth.requestMatchers("/api/**").permitAll();
                    } else {
                        // Administración: consulta para los tres roles; cambios solo ADMIN.
                        auth.requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categorias",
                                "/api/v1/categorias/**",
                                "/api/v1/productos",
                                "/api/v1/productos/**")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        "/api/v1/categorias",
                                        "/api/v1/categorias/**",
                                        "/api/v1/productos",
                                        "/api/v1/productos/**").hasAnyRole("ADMIN", "GERENTE_GENERAL")

                        // Inventario: VENDEDOR solo consulta existencias para registrar ventas.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/existencias",
                                        "/api/v1/existencias/producto/*")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/existencias",
                                        "/api/v1/existencias/*",
                                        "/api/v1/existencias/**")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/insumos",
                                        "/api/v1/insumos/**")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        "/api/v1/insumos",
                                        "/api/v1/insumos/**",
                                        "/api/v1/existencias",
                                        "/api/v1/existencias/**").hasAnyRole("ADMIN", "GERENTE_GENERAL")

                        // Compras: consulta SUPERVISOR/ADMIN; mutaciones y anulación ADMIN.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/compras",
                                        "/api/v1/compras/**",
                                        "/api/v1/proveedores",
                                        "/api/v1/proveedores/**")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/compras/*/anular").hasAnyRole("ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        "/api/v1/compras",
                                        "/api/v1/compras/**",
                                        "/api/v1/proveedores",
                                        "/api/v1/proveedores/**").hasAnyRole("ADMIN", "GERENTE_GENERAL")

                        // Producción: SUPERVISOR consulta; ADMIN gestiona.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/ordenes-produccion",
                                        "/api/v1/ordenes-produccion/**")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(
                                        "/api/v1/ordenes-produccion",
                                        "/api/v1/ordenes-produccion/**").hasAnyRole("ADMIN", "GERENTE_GENERAL")

                        // Finanzas: solo consulta SUPERVISOR/ADMIN.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/movimientos-financieros",
                                        "/api/v1/movimientos-financieros/**",
                                        "/api/v1/reportes/finanzas",
                                        "/api/v1/reportes/finanzas/**")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")

                        // Clientes: los tres roles pueden consultar y gestionar.
                                .requestMatchers(
                                        "/api/v1/clientes",
                                        "/api/v1/clientes/**")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")

                        // Ventas: VENDEDOR y SUPERVISOR registran; ADMIN solo consulta.
                                .requestMatchers(HttpMethod.POST, "/api/v1/ventas")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.GET, "/api/v1/ventas/resumen")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.GET, "/api/v1/reportes/ventas")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.GET, "/api/v1/reportes/ventas/**")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.PATCH, "/api/v1/ventas/*/anular")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")

                        // Pagos: los tres registran y consultan; solo SUPERVISOR/ADMIN anulan.
                                .requestMatchers(HttpMethod.POST, "/api/v1/ventas/*/pagos")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.GET, "/api/v1/ventas/*/pagos", "/api/v1/ventas/*/pagos/**")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.PATCH, "/api/v1/pagos-venta/*/anular")
                                .hasAnyRole("SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.GET, "/api/v1/pagos-venta", "/api/v1/pagos-venta/**")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")
                                .requestMatchers(HttpMethod.GET, "/api/v1/ventas", "/api/v1/ventas/**")
                                .hasAnyRole("VENDEDOR", "SUPERVISOR", "ADMIN", "GERENTE_GENERAL")

                                // Cobertura futura: el gerente puede operar cualquier nuevo endpoint funcional.
                                .requestMatchers("/api/**").hasRole("GERENTE_GENERAL")

                                .requestMatchers("/api/**").denyAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::realmRolesToAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> realmRolesToAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object realmAccessClaim = jwt.getClaims().get(REALM_ACCESS);
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return authorities;
        }
        Object roles = realmAccess.get(ROLES);
        if (roles instanceof Collection<?> roleList) {
            for (Object role : roleList) {
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
                }
            }
        }
        return authorities;
    }
}
