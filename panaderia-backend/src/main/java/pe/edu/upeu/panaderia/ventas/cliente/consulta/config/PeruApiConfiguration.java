package pe.edu.upeu.panaderia.ventas.cliente.consulta.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PeruApiProperties.class)
public class PeruApiConfiguration {
}
