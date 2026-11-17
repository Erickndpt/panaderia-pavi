package pe.edu.upeu.panaderia.ventas.cliente.controller;

import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.exception.ClienteConVentasException;
import pe.edu.upeu.panaderia.exception.ClienteDocumentoDuplicadoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteRequest;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResponse;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;
import pe.edu.upeu.panaderia.ventas.cliente.service.ClienteService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ClienteController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    private ClienteRequest clienteRequestValido() {
        ClienteRequest request = new ClienteRequest();
        request.setTipoDocumento(TipoDocumento.DNI);
        request.setNumeroDocumento("87654321");
        request.setNombreRazonSocial("Panadería Pavi");
        request.setTelefono("987654321");
        request.setEmail("ventas@panaderiapavi.pe");
        request.setDireccion("Av. Perú 123");
        request.setPais("Perú");
        request.setActivo(true);
        return request;
    }

    private ClienteResponse clienteResponseValido() {
        return ClienteResponse.builder()
                .id(1L)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("87654321")
                .nombreRazonSocial("Panadería Pavi")
                .telefono("987654321")
                .email("ventas@panaderiapavi.pe")
                .direccion("Av. Perú 123")
                .pais("Perú")
                .activo(true)
                .build();
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("GET /api/v1/clientes responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(clienteService.listar(null)).thenReturn(List.of(clienteResponseValido()));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroDocumento").value("87654321"));
    }

    @Test
    @DisplayName("GET /api/v1/clientes?buscar=... responde 200 delegando el filtro")
    void listarConBuscarDeberiaResponder200() throws Exception {
        when(clienteService.listar("pavi")).thenReturn(List.of(clienteResponseValido()));

        mockMvc.perform(get("/api/v1/clientes").param("buscar", "pavi"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(clienteService.obtener(1L)).thenReturn(clienteResponseValido());

        mockMvc.perform(get("/api/v1/clientes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreRazonSocial").value("Panadería Pavi"));
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(clienteService.obtener(99L)).thenThrow(new ResourceNotFoundException("Cliente", 99L));

        mockMvc.perform(get("/api/v1/clientes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/clientes con datos válidos responde 201")
    void crearDeberiaResponder201() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class))).thenReturn(clienteResponseValido());

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(clienteRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST con DNI de menos de 8 dígitos responde 400")
    void crearConDniInvalidoDeberiaResponder400() throws Exception {
        ClienteRequest request = clienteRequestValido();
        request.setNumeroDocumento("1234567");

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "El DNI debe contener exactamente 8 dígitos")));
    }

    @Test
    @DisplayName("POST con RUC que no tiene 11 dígitos responde 400")
    void crearConRucInvalidoDeberiaResponder400() throws Exception {
        ClienteRequest request = clienteRequestValido();
        request.setTipoDocumento(TipoDocumento.RUC);
        request.setNumeroDocumento("2012345678");

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "El RUC debe contener exactamente 11 dígitos")));
    }

    @ParameterizedTest(name = "tipo {0} con documento \"{1}\" responde 400")
    @CsvSource({
            "CE, AB",
            "PASAPORTE, AB",
            "SIN_DOCUMENTO, 87654321"
    })
    @DisplayName("POST con documento inválido para el tipo elegido responde 400")
    void crearConDocumentoInvalidoParaElTipoDeberiaResponder400(String tipo, String numeroDocumento) throws Exception {
        ClienteRequest request = clienteRequestValido();
        request.setTipoDocumento(TipoDocumento.valueOf(tipo));
        request.setNumeroDocumento(numeroDocumento);

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con email inválido responde 400")
    void crearConEmailInvalidoDeberiaResponder400() throws Exception {
        ClienteRequest request = clienteRequestValido();
        request.setEmail("correo-invalido");

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST sin tipo de documento responde 400")
    void crearSinTipoDocumentoDeberiaResponder400() throws Exception {
        ClienteRequest request = clienteRequestValido();
        request.setTipoDocumento(null);

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con documento duplicado responde 409")
    void crearConDocumentoDuplicadoDeberiaResponder409() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class)))
                .thenThrow(new ClienteDocumentoDuplicadoException("87654321"));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(clienteRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Ya existe un cliente con el número de documento 87654321"));
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} con datos válidos responde 200")
    void actualizarDeberiaResponder200() throws Exception {
        when(clienteService.actualizar(eq(1L), any(ClienteRequest.class))).thenReturn(clienteResponseValido());

        mockMvc.perform(put("/api/v1/clientes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(clienteRequestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} inexistente responde 404")
    void actualizarInexistenteDeberiaResponder404() throws Exception {
        when(clienteService.actualizar(eq(99L), any(ClienteRequest.class)))
                .thenThrow(new ResourceNotFoundException("Cliente", 99L));

        mockMvc.perform(put("/api/v1/clientes/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(clienteRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} válido responde 204")
    void eliminarDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/v1/clientes/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} inexistente responde 404")
    void eliminarInexistenteDeberiaResponder404() throws Exception {
        doThrow(new ResourceNotFoundException("Cliente", 99L))
                .when(clienteService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/clientes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE de un cliente con ventas asociadas responde 409")
    void eliminarConVentasDeberiaResponder409() throws Exception {
        doThrow(new ClienteConVentasException(1L))
                .when(clienteService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/clientes/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "El cliente con id 1 tiene ventas asociadas y no puede eliminarse"));
    }
}
