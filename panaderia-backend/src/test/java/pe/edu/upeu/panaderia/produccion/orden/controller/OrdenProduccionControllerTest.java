package pe.edu.upeu.panaderia.produccion.orden.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import pe.edu.upeu.panaderia.exception.InsumoStockInsuficienteException;
import pe.edu.upeu.panaderia.exception.OrdenProduccionInvalidaException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.produccion.orden.dto.DetalleConsumoRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.DetalleConsumoResponse;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionResponse;
import pe.edu.upeu.panaderia.produccion.orden.entity.EstadoOrdenProduccion;
import pe.edu.upeu.panaderia.produccion.orden.service.OrdenProduccionService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(OrdenProduccionController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class OrdenProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrdenProduccionService ordenProduccionService;

    private OrdenProduccionRequest ordenRequestValido() {
        return new OrdenProduccionRequest(
                1L,
                new BigDecimal("12"),
                List.of(new DetalleConsumoRequest(1L, new BigDecimal("2.500"))));
    }

    private OrdenProduccionResponse ordenResponseValido() {
        return new OrdenProduccionResponse(
                1L,
                LocalDateTime.of(2026, 9, 14, 10, 30),
                EstadoOrdenProduccion.COMPLETADA,
                1L,
                "Pan francés",
                new BigDecimal("12"),
                List.of(new DetalleConsumoResponse(
                        1L, 1L, "Harina de trigo", "KILOGRAMO", new BigDecimal("2.500"))));
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("POST /api/v1/ordenes-produccion con datos válidos responde 201")
    void registrarDeberiaResponder201() throws Exception {
        when(ordenProduccionService.registrar(any(OrdenProduccionRequest.class)))
                .thenReturn(ordenResponseValido());

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ordenRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.nombreProducto").value("Pan francés"))
                .andExpect(jsonPath("$.detalles[0].nombreInsumo").value("Harina de trigo"));
    }

    @Test
    @DisplayName("POST con insumos vacíos responde 400")
    void registrarConInsumosVaciosDeberiaResponder400() throws Exception {
        OrdenProduccionRequest request = new OrdenProduccionRequest(
                1L, new BigDecimal("12"), List.of());

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con cantidad producida cero responde 400")
    void registrarConCantidadProducidaCeroDeberiaResponder400() throws Exception {
        OrdenProduccionRequest request = new OrdenProduccionRequest(
                1L,
                BigDecimal.ZERO,
                List.of(new DetalleConsumoRequest(1L, new BigDecimal("2.500"))));

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con cantidad de más de tres decimales responde 400")
    void registrarConCantidadDeCuatroDecimalesDeberiaResponder400() throws Exception {
        OrdenProduccionRequest request = new OrdenProduccionRequest(
                1L,
                new BigDecimal("12"),
                List.of(new DetalleConsumoRequest(1L, new BigDecimal("2.1234"))));

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con insumo duplicado responde 400")
    void registrarConInsumoDuplicadoDeberiaResponder400() throws Exception {
        when(ordenProduccionService.registrar(any(OrdenProduccionRequest.class)))
                .thenThrow(new OrdenProduccionInvalidaException(
                        "El insumo 1 aparece más de una vez en la orden"));

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ordenRequestValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST con producto inexistente responde 404")
    void registrarConProductoInexistenteDeberiaResponder404() throws Exception {
        when(ordenProduccionService.registrar(any(OrdenProduccionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Producto", 99L));

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ordenRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST con insumo inexistente responde 404")
    void registrarConInsumoInexistenteDeberiaResponder404() throws Exception {
        when(ordenProduccionService.registrar(any(OrdenProduccionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Insumo", 99L));

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ordenRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST con stock insuficiente de un insumo responde 409")
    void registrarConStockInsuficienteDeberiaResponder409() throws Exception {
        when(ordenProduccionService.registrar(any(OrdenProduccionRequest.class)))
                .thenThrow(new InsumoStockInsuficienteException(
                        1L, new BigDecimal("20"), new BigDecimal("10")));

        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ordenRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Stock insuficiente para el insumo 1: se solicitó 20 y hay 10 disponibles"));
    }

    @Test
    @DisplayName("GET /api/v1/ordenes-produccion responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(ordenProduccionService.listar()).thenReturn(List.of(ordenResponseValido()));

        mockMvc.perform(get("/api/v1/ordenes-produccion"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ordenes-produccion/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(ordenProduccionService.obtener(1L)).thenReturn(ordenResponseValido());

        mockMvc.perform(get("/api/v1/ordenes-produccion/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreProducto").value("Pan francés"))
                .andExpect(jsonPath("$.detalles[0].cantidadConsumida").value(2.5));
    }

    @Test
    @DisplayName("GET /api/v1/ordenes-produccion/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        doThrow(new ResourceNotFoundException("Orden de producción", 99L))
                .when(ordenProduccionService).obtener(99L);

        mockMvc.perform(get("/api/v1/ordenes-produccion/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}