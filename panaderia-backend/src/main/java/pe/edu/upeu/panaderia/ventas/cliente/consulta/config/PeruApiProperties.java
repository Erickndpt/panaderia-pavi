package pe.edu.upeu.panaderia.ventas.cliente.consulta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "peru-api")
public record PeruApiProperties(String baseUrl, String apiKey) {
}
