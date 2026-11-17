package pe.edu.upeu.panaderia.administracion.producto.controller;

import java.math.BigDecimal;
import java.util.List;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResumen;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoRequest;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductoController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductoService productoService;

    private ProductoRequest productoRequestValido() {
        ProductoRequest request = new ProductoRequest();
        request.setNombre("Pan Integral");
        request.setPrecio(new BigDecimal("3.50"));
        request.setCategoriaId(1L);
        return request;
    }

    private ProductoResponse productoResponseValido() {
        return ProductoResponse.builder()
                .id(1L)
                .nombre("Pan Integral")
                .precio(new BigDecimal("3.50"))
                .categoria(new CategoriaResumen(1L, "Panificación"))
                .build();
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("GET /api/v1/productos responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(productoService.listar(null, null, null, null)).thenReturn(List.of(productoResponseValido()));

        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/productos con datos válidos responde 201")
    void crearDeberiaResponder201() throws Exception {
        when(productoService.crear(any(ProductoRequest.class))).thenReturn(productoResponseValido());

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productoRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoria.id").value(1))
                .andExpect(jsonPath("$.categoria.nombre").value("Panificación"));
    }

    @Test
    @DisplayName("POST con nombre vacío responde 400")
    void crearConNombreVacioDeberiaResponder400() throws Exception {
        ProductoRequest request = productoRequestValido();
        request.setNombre("");

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con precio negativo responde 400")
    void crearConPrecioNegativoDeberiaResponder400() throws Exception {
        ProductoRequest request = productoRequestValido();
        request.setPrecio(new BigDecimal("-1.00"));

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(productoService.obtener(99L)).thenThrow(new ResourceNotFoundException("Producto", 99L));

        mockMvc.perform(get("/api/v1/productos/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/productos/{id} con datos válidos responde 200")
    void actualizarDeberiaResponder200() throws Exception {
        when(productoService.actualizar(eq(1L), any(ProductoRequest.class))).thenReturn(productoResponseValido());

        mockMvc.perform(put("/api/v1/productos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productoRequestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/productos/{id} válido responde 204")
    void eliminarDeberiaResponder204() throws Exception {
        mockMvc.perform(delete("/api/v1/productos/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/productos?categoriaId=1 responde 200")
    void listarPorCategoriaDeberiaResponder200() throws Exception {
        when(productoService.listar(null, 1L, null, null)).thenReturn(List.of(productoResponseValido()));

        mockMvc.perform(get("/api/v1/productos").param("categoriaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/productos?categoriaId=99 inexistente responde 404")
    void listarPorCategoriaInexistenteDeberiaResponder404() throws Exception {
        when(productoService.listar(null, 99L, null, null))
                .thenThrow(new ResourceNotFoundException("Categoría", 99L));

        mockMvc.perform(get("/api/v1/productos").param("categoriaId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/productos con filtros combinados responde 200")
    void listarConFiltroCombinadoDeberiaResponder200() throws Exception {
        when(productoService.listar("integral", 1L, new BigDecimal("2"), new BigDecimal("10")))
                .thenReturn(List.of(productoResponseValido()));

        mockMvc.perform(get("/api/v1/productos")
                        .param("nombre", "integral")
                        .param("categoriaId", "1")
                        .param("precioMin", "2")
                        .param("precioMax", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/productos con precioMin mayor que precioMax responde 400")
    void listarConRangoDePreciosInvalidoDeberiaResponder400() throws Exception {
        when(productoService.listar(any(), isNull(), any(BigDecimal.class), any(BigDecimal.class)))
                .thenThrow(new FiltroInvalidoException("precioMin no debe ser mayor que precioMax"));

        mockMvc.perform(get("/api/v1/productos")
                        .param("precioMin", "10")
                        .param("precioMax", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("precioMin no debe ser mayor que precioMax"));
    }

    @Test
    @DisplayName("Al enviar X-Trace-ID, la respuesta devuelve el mismo valor")
    void trazaEnviadaSeDevuelveEnLaRespuesta() throws Exception {
        mockMvc.perform(get("/api/v1/productos").header("X-Trace-ID", "traza-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", "traza-123"));
    }

    @Test
    @DisplayName("Sin X-Trace-ID, la respuesta contiene un id de traza no vacío")
    void trazaGeneradaCuandoNoSeEnviaHeader() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", not(emptyString())));
    }
}