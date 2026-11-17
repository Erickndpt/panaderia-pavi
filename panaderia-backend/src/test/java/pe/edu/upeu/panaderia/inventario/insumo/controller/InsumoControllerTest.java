package pe.edu.upeu.panaderia.inventario.insumo.controller;

import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import pe.edu.upeu.panaderia.exception.InsumoDuplicadoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoRequest;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(InsumoController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class InsumoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InsumoService insumoService;

    private InsumoRequest insumoRequestValido() {
        InsumoRequest request = new InsumoRequest();
        request.setNombre("Harina de trigo");
        request.setUnidadMedida("KILOGRAMO");
        request.setStockActual(new BigDecimal("50.000"));
        request.setStockMinimo(new BigDecimal("10.000"));
        return request;
    }

    private InsumoResponse insumoResponseValido() {
        return InsumoResponse.builder()
                .id(1L)
                .nombre("Harina de trigo")
                .unidadMedida("KILOGRAMO")
                .stockActual(new BigDecimal("50.000"))
                .stockMinimo(new BigDecimal("10.000"))
                .build();
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("GET /api/v1/insumos responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(insumoService.listar()).thenReturn(List.of(insumoResponseValido()));

        mockMvc.perform(get("/api/v1/insumos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/insumos/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(insumoService.obtener(1L)).thenReturn(insumoResponseValido());

        mockMvc.perform(get("/api/v1/insumos/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Harina de trigo"))
                .andExpect(jsonPath("$.unidadMedida").value("KILOGRAMO"));
    }

    @Test
    @DisplayName("GET /api/v1/insumos/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(insumoService.obtener(99L)).thenThrow(new ResourceNotFoundException("Insumo", 99L));

        mockMvc.perform(get("/api/v1/insumos/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/insumos con datos válidos responde 201")
    void crearDeberiaResponder201() throws Exception {
        when(insumoService.crear(any(InsumoRequest.class))).thenReturn(insumoResponseValido());

        mockMvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(insumoRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST con nombre vacío responde 400")
    void crearConNombreVacioDeberiaResponder400() throws Exception {
        InsumoRequest request = insumoRequestValido();
        request.setNombre("");

        mockMvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con stockActual nulo responde 400")
    void crearConStockActualNuloDeberiaResponder400() throws Exception {
        InsumoRequest request = insumoRequestValido();
        request.setStockActual(null);

        mockMvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con nombre duplicado responde 409")
    void crearConNombreDuplicadoDeberiaResponder409() throws Exception {
        when(insumoService.crear(any(InsumoRequest.class)))
                .thenThrow(new InsumoDuplicadoException("Harina de trigo"));

        mockMvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(insumoRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un insumo con el nombre Harina de trigo"));
    }

    @Test
    @DisplayName("PUT /api/v1/insumos/{id} con datos válidos responde 200")
    void actualizarDeberiaResponder200() throws Exception {
        when(insumoService.actualizar(eq(1L), any(InsumoRequest.class))).thenReturn(insumoResponseValido());

        mockMvc.perform(put("/api/v1/insumos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(insumoRequestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/insumos/{id} inexistente responde 404")
    void actualizarInexistenteDeberiaResponder404() throws Exception {
        when(insumoService.actualizar(eq(99L), any(InsumoRequest.class)))
                .thenThrow(new ResourceNotFoundException("Insumo", 99L));

        mockMvc.perform(put("/api/v1/insumos/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(insumoRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT con nombre usado por otro insumo responde 409")
    void actualizarConNombreDuplicadoDeberiaResponder409() throws Exception {
        when(insumoService.actualizar(eq(1L), any(InsumoRequest.class)))
                .thenThrow(new InsumoDuplicadoException("Harina de trigo"));

        mockMvc.perform(put("/api/v1/insumos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(insumoRequestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/insumos/{id} válido responde 204")
    void eliminarDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/v1/insumos/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}