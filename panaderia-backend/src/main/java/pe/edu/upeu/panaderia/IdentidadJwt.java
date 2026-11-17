package pe.edu.upeu.panaderia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Identidad mínima del usuario autenticado que necesita el módulo Ventas.
 * El id es el claim sub; username y nombre se usan solo para mostrar al
 * vendedor sin exponer el UUID en la interfaz.
 */
public record IdentidadJwt(String id, String username, String nombre, boolean vendedor) {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    public static IdentidadJwt desde(Jwt jwt) {
        if (jwt == null) {
            return anonima();
        }

        String username = texto(jwt.getClaimAsString("preferred_username"));
        String nombre = texto(jwt.getClaimAsString("name"));
        if (nombre == null) {
            nombre = componerNombre(
                    texto(jwt.getClaimAsString("given_name")),
                    texto(jwt.getClaimAsString("family_name")));
        }
        if (nombre == null) {
            nombre = username;
        }

        return new IdentidadJwt(
                texto(jwt.getSubject()),
                username,
                nombre,
                roles(jwt).contains("VENDEDOR"));
    }

    public static IdentidadJwt anonima() {
        return new IdentidadJwt(null, null, null, false);
    }

    private static List<String> roles(Jwt jwt) {
        List<String> resultado = new ArrayList<>();
        Object realmAccessClaim = jwt.getClaims().get(REALM_ACCESS);
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return resultado;
        }
        Object rolesClaim = realmAccess.get(ROLES);
        if (rolesClaim instanceof Collection<?> lista) {
            for (Object rol : lista) {
                if (rol != null) {
                    resultado.add(rol.toString());
                }
            }
        }
        return resultado;
    }

    private static String componerNombre(String nombres, String apellidos) {
        if (nombres == null) {
            return apellidos;
        }
        if (apellidos == null) {
            return nombres;
        }
        return nombres + " " + apellidos;
    }

    private static String texto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
