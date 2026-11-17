package pe.edu.upeu.panaderia.compras.proveedor.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResponse;
import pe.edu.upeu.panaderia.compras.proveedor.service.ProveedorService;
import pe.edu.upeu.panaderia.exception.ProveedorDuplicadoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProveedorController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProveedorService proveedorService;

    private ProveedorRequest proveedorRequestValido() {
        ProveedorRequest request = new ProveedorRequest();
        request.setRuc("20123456789");
        request.setRazonSocial("Molino La Estrella S.A.C.");
        request.setTelefono("987654321");
        request.setEmail("contacto@molinoestrella.pe");
        request.setActivo(true);
        return request;
    }

    private ProveedorResponse proveedorResponseValido() {
        return ProveedorResponse.builder()
                .id(1L)
                .ruc("20123456789")
                .razonSocial("Molino La Estrella S.A.C.")
                .telefono("987654321")
                .email("contacto@molinoestrella.pe")
                .activo(true)
                .build();
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("GET /api/v1/proveedores responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(proveedorService.listar()).thenReturn(List.of(proveedorResponseValido()));

        mockMvc.perform(get("/api/v1/proveedores"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/proveedores/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(proveedorService.obtener(1L)).thenReturn(proveedorResponseValido());

        mockMvc.perform(get("/api/v1/proveedores/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20123456789"))
                .andExpect(jsonPath("$.razonSocial").value("Molino La Estrella S.A.C."));
    }

    @Test
    @DisplayName("GET /api/v1/proveedores/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(proveedorService.obtener(99L)).thenThrow(new ResourceNotFoundException("Proveedor", 99L));

        mockMvc.perform(get("/api/v1/proveedores/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/proveedores con datos válidos responde 201")
    void crearDeberiaResponder201() throws Exception {
        when(proveedorService.crear(any(ProveedorRequest.class))).thenReturn(proveedorResponseValido());

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(proveedorRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST con RUC de menos de 11 dígitos responde 400")
    void crearConRucInvalidoDeberiaResponder400() throws Exception {
        ProveedorRequest request = proveedorRequestValido();
        request.setRuc("12345");

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con RUC con letras responde 400")
    void crearConRucNoNumericoDeberiaResponder400() throws Exception {
        ProveedorRequest request = proveedorRequestValido();
        request.setRuc("20ABCDEFGHI");

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con razón social vacía responde 400")
    void crearConRazonSocialVaciaDeberiaResponder400() throws Exception {
        ProveedorRequest request = proveedorRequestValido();
        request.setRazonSocial("");

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con RUC duplicado responde 409")
    void crearConRucDuplicadoDeberiaResponder409() throws Exception {
        when(proveedorService.crear(any(ProveedorRequest.class)))
                .thenThrow(new ProveedorDuplicadoException("20123456789"));

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(proveedorRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un proveedor con el RUC 20123456789"));
    }

    @Test
    @DisplayName("PUT /api/v1/proveedores/{id} con datos válidos responde 200")
    void actualizarDeberiaResponder200() throws Exception {
        when(proveedorService.actualizar(eq(1L), any(ProveedorRequest.class))).thenReturn(proveedorResponseValido());

        mockMvc.perform(put("/api/v1/proveedores/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(proveedorRequestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/proveedores/{id} inexistente responde 404")
    void actualizarInexistenteDeberiaResponder404() throws Exception {
        when(proveedorService.actualizar(eq(99L), any(ProveedorRequest.class)))
                .thenThrow(new ResourceNotFoundException("Proveedor", 99L));

        mockMvc.perform(put("/api/v1/proveedores/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(proveedorRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT con RUC usado por otro proveedor responde 409")
    void actualizarConRucDuplicadoDeberiaResponder409() throws Exception {
        when(proveedorService.actualizar(eq(1L), any(ProveedorRequest.class)))
                .thenThrow(new ProveedorDuplicadoException("20123456789"));

        mockMvc.perform(put("/api/v1/proveedores/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(proveedorRequestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/proveedores/{id} válido responde 204")
    void eliminarDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/v1/proveedores/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/proveedores/{id} inexistente responde 404")
    void eliminarInexistenteDeberiaResponder404() throws Exception {
        doThrow(new ResourceNotFoundException("Proveedor", 99L))
                .when(proveedorService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/proveedores/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}