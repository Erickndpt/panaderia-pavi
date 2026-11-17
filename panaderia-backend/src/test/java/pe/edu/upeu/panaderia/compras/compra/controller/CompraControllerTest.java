package pe.edu.upeu.panaderia.compras.compra.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import pe.edu.upeu.panaderia.compras.compra.dto.CompraRequest;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraResponse;
import pe.edu.upeu.panaderia.compras.compra.dto.DetalleCompraRequest;
import pe.edu.upeu.panaderia.compras.compra.dto.DetalleCompraResponse;
import pe.edu.upeu.panaderia.compras.compra.entity.EstadoCompra;
import pe.edu.upeu.panaderia.compras.compra.service.CompraService;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResumen;
import pe.edu.upeu.panaderia.exception.CompraYaAnuladaException;
import pe.edu.upeu.panaderia.exception.InsumoStockInsuficienteException;
import pe.edu.upeu.panaderia.exception.ProveedorInactivoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CompraController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class CompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompraService compraService;

    private CompraRequest compraRequestValido() {
        return new CompraRequest(
                1L,
                List.of(new DetalleCompraRequest(1L, new BigDecimal("2"), new BigDecimal("3.50"))));
    }

    private CompraResponse compraResponseValido() {
        return new CompraResponse(
                1L,
                new ProveedorResumen(1L, "Molino La Estrella S.A.C."),
                LocalDateTime.of(2026, 9, 14, 10, 30),
                EstadoCompra.REGISTRADA,
                new BigDecimal("7.00"),
                List.of(new DetalleCompraResponse(
                        1L, 1L, "Harina de trigo", "KILOGRAMO", new BigDecimal("2"),
                        new BigDecimal("3.50"), new BigDecimal("7.00"))));
    }

    private CompraResponse compraResponseAnulada() {
        return new CompraResponse(
                1L,
                new ProveedorResumen(1L, "Molino La Estrella S.A.C."),
                LocalDateTime.of(2026, 9, 14, 10, 30),
                EstadoCompra.ANULADA,
                new BigDecimal("7.00"),
                List.of(new DetalleCompraResponse(
                        1L, 1L, "Harina de trigo", "KILOGRAMO", new BigDecimal("2"),
                        new BigDecimal("3.50"), new BigDecimal("7.00"))));
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("POST /api/v1/compras con datos válidos responde 201")
    void registrarDeberiaResponder201() throws Exception {
        when(compraService.registrar(any(CompraRequest.class))).thenReturn(compraResponseValido());

        mockMvc.perform(post("/api/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(compraRequestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.total").value(7.00))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"));
    }

    @Test
    @DisplayName("POST con detalles vacíos responde 400")
    void registrarConDetallesVaciosDeberiaResponder400() throws Exception {
        CompraRequest request = new CompraRequest(1L, List.of());

        mockMvc.perform(post("/api/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con cantidad cero responde 400")
    void registrarConCantidadCeroDeberiaResponder400() throws Exception {
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new DetalleCompraRequest(1L, BigDecimal.ZERO, new BigDecimal("3.50"))));

        mockMvc.perform(post("/api/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con proveedor inexistente responde 404")
    void registrarConProveedorInexistenteDeberiaResponder404() throws Exception {
        when(compraService.registrar(any(CompraRequest.class)))
                .thenThrow(new ResourceNotFoundException("Proveedor", 99L));

        mockMvc.perform(post("/api/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(compraRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST con proveedor inactivo responde 409")
    void registrarConProveedorInactivoDeberiaResponder409() throws Exception {
        when(compraService.registrar(any(CompraRequest.class)))
                .thenThrow(new ProveedorInactivoException(1L));

        mockMvc.perform(post("/api/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(compraRequestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST con insumo inexistente responde 404")
    void registrarConInsumoInexistenteDeberiaResponder404() throws Exception {
        when(compraService.registrar(any(CompraRequest.class)))
                .thenThrow(new ResourceNotFoundException("Insumo", 99L));

        mockMvc.perform(post("/api/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(compraRequestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/compras responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(compraService.listar()).thenReturn(List.of(compraResponseValido()));

        mockMvc.perform(get("/api/v1/compras"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/compras/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(compraService.obtener(1L)).thenReturn(compraResponseValido());

        mockMvc.perform(get("/api/v1/compras/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedor.razonSocial").value("Molino La Estrella S.A.C."))
                .andExpect(jsonPath("$.detalles[0].nombreInsumo").value("Harina de trigo"));
    }

    @Test
    @DisplayName("GET /api/v1/compras/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        doThrow(new ResourceNotFoundException("Compra", 99L))
                .when(compraService).obtener(99L);

        mockMvc.perform(get("/api/v1/compras/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/compras/{id}/anular responde 200 con la compra ANULADA")
    void anularDeberiaResponder200() throws Exception {
        when(compraService.anular(1L)).thenReturn(compraResponseAnulada());

        mockMvc.perform(patch("/api/v1/compras/{id}/anular", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    @Test
    @DisplayName("PATCH anular compra inexistente responde 404")
    void anularCompraInexistenteDeberiaResponder404() throws Exception {
        when(compraService.anular(99L))
                .thenThrow(new ResourceNotFoundException("Compra", 99L));

        mockMvc.perform(patch("/api/v1/compras/{id}/anular", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH anular compra ya anulada responde 409")
    void anularCompraYaAnuladaDeberiaResponder409() throws Exception {
        when(compraService.anular(1L)).thenThrow(new CompraYaAnuladaException(1L));

        mockMvc.perform(patch("/api/v1/compras/{id}/anular", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PATCH anular con stock insuficiente responde 409")
    void anularConStockInsuficienteDeberiaResponder409() throws Exception {
        when(compraService.anular(1L))
                .thenThrow(new InsumoStockInsuficienteException(2L, new BigDecimal("5"), new BigDecimal("2")));

        mockMvc.perform(patch("/api/v1/compras/{id}/anular", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Stock insuficiente para el insumo 2: se solicitó 5 y hay 2 disponibles"));
    }

    @Test
    @DisplayName("PATCH anular con fallo del movimiento financiero responde 404")
    void anularConFalloFinancieroDeberiaResponder404() throws Exception {
        when(compraService.anular(1L))
                .thenThrow(new ResourceNotFoundException("Movimiento financiero de la compra", 1L));

        mockMvc.perform(patch("/api/v1/compras/{id}/anular", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Movimiento financiero de la compra con id 1 no encontrado"));
    }
}