package pe.edu.upeu.panaderia.ventas.cliente.consulta.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.config.PeruApiProperties;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.dto.DocumentoConsultaResponse;

class PeruApiClientTest {

    private MockRestServiceServer server;
    private PeruApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PeruApiClient(
                builder.baseUrl("https://peruapi.com").build(),
                new PeruApiProperties("https://peruapi.com", "clave-de-prueba"));
    }

    @Test
    void consultaDniMapeaLaRespuestaOficialSinConsumirLaApiReal() {
        server.expect(requestTo(
                        "https://peruapi.com/api/dni/60012345?summary=0&plan=0"))
                .andExpect(header("X-API-KEY", "clave-de-prueba"))
                .andRespond(withSuccess("""
                        {
                          "dni": "60012345",
                          "cliente": "QUISPE PEREZ JULIO FERNANDO",
                          "nombres": "JULIO FERNANDO",
                          "apellido_paterno": "QUISPE",
                          "apellido_materno": "PEREZ",
                          "mensaje": "OK",
                          "code": "200"
                        }
                        """, MediaType.APPLICATION_JSON));

        DocumentoConsultaResponse response = client.consultarDni("60012345");

        assertThat(response.tipoDocumento()).isEqualTo("DNI");
        assertThat(response.numeroDocumento()).isEqualTo("60012345");
        assertThat(response.nombreRazonSocial()).isEqualTo("QUISPE PEREZ JULIO FERNANDO");
        assertThat(response.pais()).isEqualTo("Perú");
        assertThat(response.direccion()).isNull();
        server.verify();
    }

    @Test
    void consultaRucMapeaRazonSocialDireccionYEstado() {
        server.expect(requestTo(
                        "https://peruapi.com/api/ruc/20100017491?summary=0&plan=0"))
                .andExpect(header("X-API-KEY", "clave-de-prueba"))
                .andRespond(withSuccess("""
                        {
                          "ruc": "20100017491",
                          "razon_social": "INTEGRATEL PERÚ S.A.A.",
                          "estado": "ACTIVO",
                          "condicion": "HABIDO",
                          "direccion": "JR. DOMINGO MARTINEZ LUJAN NRO. 1130",
                          "mensaje": "OK",
                          "code": "200"
                        }
                        """, MediaType.APPLICATION_JSON));

        DocumentoConsultaResponse response = client.consultarRuc("20100017491");

        assertThat(response.tipoDocumento()).isEqualTo("RUC");
        assertThat(response.nombreRazonSocial()).isEqualTo("INTEGRATEL PERÚ S.A.A.");
        assertThat(response.direccion()).isEqualTo("JR. DOMINGO MARTINEZ LUJAN NRO. 1130");
        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.condicion()).isEqualTo("HABIDO");
        server.verify();
    }
}
