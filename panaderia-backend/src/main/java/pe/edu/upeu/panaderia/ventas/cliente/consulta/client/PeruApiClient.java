package pe.edu.upeu.panaderia.ventas.cliente.consulta.client;

import tools.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.config.PeruApiProperties;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.dto.DocumentoConsultaResponse;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaBadRequestException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaNotFoundException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaRateLimitedException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaServiceUnavailableException;
import pe.edu.upeu.panaderia.ventas.cliente.consulta.exception.DocumentoConsultaUnauthorizedException;

@Component
@RequiredArgsConstructor
public class PeruApiClient {

    private static final String PAIS_PERU = "Perú";
    private static final String MENSAJE_NO_ENCONTRADO =
            "No se encontró información para el documento ingresado.";
    private static final String MENSAJE_NO_DISPONIBLE =
            "El servicio de consulta no está disponible. Puedes completar los datos manualmente.";

    private final RestClient peruApiRestClient;
    private final PeruApiProperties properties;

    public DocumentoConsultaResponse consultarDni(String dni) {
        JsonNode response = consultar("/api/dni/{numero}", dni);

        String nombreCompleto = texto(response, "cliente");
        if (nombreCompleto == null) {
            nombreCompleto = Stream.of(
                            texto(response, "apellido_paterno"),
                            texto(response, "apellido_materno"),
                            texto(response, "nombres"))
                    .filter(value -> value != null && !value.isBlank())
                    .reduce((left, right) -> left + " " + right)
                    .orElse(null);
        }

        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new DocumentoConsultaNotFoundException(MENSAJE_NO_ENCONTRADO);
        }

        return new DocumentoConsultaResponse(
                "DNI", dni, nombreCompleto, null, PAIS_PERU, null, null);
    }

    public DocumentoConsultaResponse consultarRuc(String ruc) {
        JsonNode response = consultar("/api/ruc/{numero}", ruc);
        String razonSocial = texto(response, "razon_social");

        if (razonSocial == null || razonSocial.isBlank()) {
            throw new DocumentoConsultaNotFoundException(MENSAJE_NO_ENCONTRADO);
        }

        return new DocumentoConsultaResponse(
                "RUC",
                ruc,
                razonSocial,
                texto(response, "direccion"),
                PAIS_PERU,
                texto(response, "estado"),
                texto(response, "condicion"));
    }

    private JsonNode consultar(String path, String numero) {
        validarApiKey();

        try {
            JsonNode response = peruApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("summary", 0)
                            .queryParam("plan", 0)
                            .build(numero))
                    .header("X-API-KEY", properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) ->
                            lanzarError(httpResponse))
                    .body(JsonNode.class);

            if (response == null || !"200".equals(texto(response, "code"))) {
                throw new DocumentoConsultaNotFoundException(MENSAJE_NO_ENCONTRADO);
            }
            return response;
        } catch (DocumentoConsultaException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new DocumentoConsultaServiceUnavailableException(MENSAJE_NO_DISPONIBLE);
        }
    }

    private void validarApiKey() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DocumentoConsultaUnauthorizedException(MENSAJE_NO_DISPONIBLE);
        }
    }

    private void lanzarError(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        if (status == 401 || status == 403) {
            throw new DocumentoConsultaUnauthorizedException(MENSAJE_NO_DISPONIBLE);
        }
        if (status == 404) {
            throw new DocumentoConsultaNotFoundException(MENSAJE_NO_ENCONTRADO);
        }
        if (status == 429) {
            throw new DocumentoConsultaRateLimitedException(
                    "Se alcanzó el límite temporal de consultas. Inténtalo más tarde.");
        }
        if (status >= 400 && status < 500) {
            throw new DocumentoConsultaBadRequestException(
                    "No se pudo realizar la consulta con los datos ingresados.");
        }
        throw new DocumentoConsultaServiceUnavailableException(MENSAJE_NO_DISPONIBLE);
    }

    private String texto(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || !value.isValueNode()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }
}





