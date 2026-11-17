package pe.edu.upeu.panaderia.ventas.cliente.consulta.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PeruApiRestClientConfig {

    private final PeruApiProperties properties;

    public PeruApiRestClientConfig(PeruApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestClient peruApiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .baseUrl(properties.baseUrl() == null ? "https://peruapi.com" : properties.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
