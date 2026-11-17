package pe.edu.upeu.panaderia.inventario.existencia.controller;

import java.math.BigDecimal;
import java.util.List;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import pe.edu.upeu.panaderia.exception.ExistenciaConStockException;
import pe.edu.upeu.panaderia.exception.ExistenciaDuplicadaException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaRequest;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaResponse;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ExistenciaController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class ExistenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExistenciaService existenciaService;

    private ExistenciaResponse existenciaResponseValida() {
        return new ExistenciaResponse(1L,
                new ExistenciaResponse.ProductoResumen(1L, "Pan de crawit"),
                new BigDecimal("100.000"));
    }

    private ExistenciaRequest existenciaRequestValido() {
        ExistenciaRequest request = new ExistenciaRequest();
        request.setProductoId(1L);
        request.setCantidad(new BigDecimal("100.000"));
        return request;
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("GET /api/v1/existencias responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(existenciaService.listar(null)).thenReturn(List.of(existenciaResponseValida()));

        mockMvc.perform(get("/api/v1/existencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].producto.id").value(1))
                .andExpect(jsonPath("$[0].producto.nombre").value("Pan de crawit"))
                .andExpect(jsonPath("$[0].cantidad").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/existencias?productoId={id} filtra en el backend")
    void listarConFiltroDeberiaResponder200() throws Exception {
        when(existenciaService.listar(1L)).thenReturn(List.of(existenciaResponseValida()));

        mockMvc.perform(get("/api/v1/existencias").param("productoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].producto.nombre").value("Pan de crawit"));

        verify(existenciaService).listar(1L);
    }

    @Test
    @DisplayName("GET /api/v1/existencias?productoId=abc responde 400")
    void listarConFiltroInvalidoDeberiaResponder400() throws Exception {
        mockMvc.perform(get("/api/v1/existencias").param("productoId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/existencias/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(existenciaService.obtener(1L)).thenReturn(existenciaResponseValida());

        mockMvc.perform(get("/api/v1/existencias/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.producto.nombre").value("Pan de crawit"))
                .andExpect(jsonPath("$.cantidad").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/existencias/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(existenciaService.obtener(99L)).thenThrow(new ResourceNotFoundException("Existencia", 99L));

        mockMvc.perform(get("/api/v1/existencias/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/existencias/producto/{id} responde 200")
    void obtenerPorProductoDeberiaResponder200() throws Exception {
        when(existenciaService.obtenerPorProducto(1L)).thenReturn(existenciaResponseValida());

        mockMvc.perform(get("/api/v1/existencias/producto/{productoId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.producto.nombre").value("Pan de crawit"))
                .andExpect(jsonPath("$.cantidad").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/existencias/producto/{id} sin existencia responde 404")
    void obtenerPorProductoInexistenteDeberiaResponder404() throws Exception {
        when(existenciaService.obtenerPorProducto(99L))
                .thenThrow(new ResourceNotFoundException("Existencia del producto", 99L));

        mockMvc.perform(get("/api/v1/existencias/producto/{productoId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/existencias con datos válidos responde 201")
    void crearDeberiaResponder201() throws Exception {
        when(existenciaService.crear(any(ExistenciaRequest.class))).thenReturn(existenciaResponseValida());

        mockMvc.perform(post("/api/v1/existencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(existenciaRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.producto.nombre").value("Pan de crawit"));
    }

    @Test
    @DisplayName("POST sin productoId responde 400")
    void crearSinProductoIdDeberiaResponder400() throws Exception {
        ExistenciaRequest request = existenciaRequestValido();
        request.setProductoId(null);

        mockMvc.perform(post("/api/v1/existencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST con productoId negativo responde 400")
    void crearConProductoIdNegativoDeberiaResponder400() throws Exception {
        ExistenciaRequest request = existenciaRequestValido();
        request.setProductoId(-5L);

        mockMvc.perform(post("/api/v1/existencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con cantidad negativa responde 400")
    void crearConCantidadNegativaDeberiaResponder400() throws Exception {
        ExistenciaRequest request = existenciaRequestValido();
        request.setCantidad(new BigDecimal("-1.000"));

        mockMvc.perform(post("/api/v1/existencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con producto inexistente responde 404")
    void crearConProductoInexistenteDeberiaResponder404() throws Exception {
        ExistenciaRequest request = existenciaRequestValido();
        request.setProductoId(99L);
        when(existenciaService.crear(any(ExistenciaRequest.class)))
                .thenThrow(new ResourceNotFoundException("Producto", 99L));

        mockMvc.perform(post("/api/v1/existencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST con producto que ya tiene existencia responde 409")
    void crearConProductoDuplicadoDeberiaResponder409() throws Exception {
        when(existenciaService.crear(any(ExistenciaRequest.class)))
                .thenThrow(new ExistenciaDuplicadaException(1L));

        mockMvc.perform(post("/api/v1/existencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(existenciaRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El producto 1 ya tiene una existencia registrada"));
    }

    @Test
    @DisplayName("PUT /api/v1/existencias/{id} con datos válidos responde 200")
    void actualizarDeberiaResponder200() throws Exception {
        when(existenciaService.actualizar(eq(1L), any(ExistenciaRequest.class))).thenReturn(existenciaResponseValida());

        mockMvc.perform(put("/api/v1/existencias/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(existenciaRequestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.producto.nombre").value("Pan de crawit"));
    }

    @Test
    @DisplayName("PUT /api/v1/existencias/{id} inexistente responde 404")
    void actualizarInexistenteDeberiaResponder404() throws Exception {
        when(existenciaService.actualizar(eq(99L), any(ExistenciaRequest.class)))
                .thenThrow(new ResourceNotFoundException("Existencia", 99L));

        mockMvc.perform(put("/api/v1/existencias/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(existenciaRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT hacia un producto que ya tiene otra existencia responde 409")
    void actualizarConProductoDuplicadoDeberiaResponder409() throws Exception {
        when(existenciaService.actualizar(eq(1L), any(ExistenciaRequest.class)))
                .thenThrow(new ExistenciaDuplicadaException(1L));

        mockMvc.perform(put("/api/v1/existencias/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(existenciaRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("DELETE /api/v1/existencias/{id} con cantidad cero responde 204")
    void eliminarConCantidadCeroDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/v1/existencias/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(existenciaService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE con cantidad positiva responde 409")
    void eliminarConCantidadPositivaDeberiaResponder409() throws Exception {
        doThrow(new ExistenciaConStockException(1L, new BigDecimal("12.500")))
                .when(existenciaService).eliminar(1L);
        mockMvc.perform(delete("/api/v1/existencias/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar la existencia del producto 1 porque tiene 12.500 disponibles. "
                                + "Solo se permite eliminar existencias con cantidad cero"));
    }

    @Test
    @DisplayName("DELETE /api/v1/existencias/{id} inexistente responde 404")
    void eliminarInexistenteDeberiaResponder404() throws Exception {
        doThrow(new ResourceNotFoundException("Existencia", 99L))
                .when(existenciaService).eliminar(99L);

        mockMvc.perform(delete("/api/v1/existencias/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Sin X-Trace-ID, la respuesta contiene un id de traza no vacío")
    void trazaGeneradaCuandoNoSeEnviaHeader() throws Exception {
        mockMvc.perform(get("/api/v1/existencias"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", not(emptyString())));
    }
}
