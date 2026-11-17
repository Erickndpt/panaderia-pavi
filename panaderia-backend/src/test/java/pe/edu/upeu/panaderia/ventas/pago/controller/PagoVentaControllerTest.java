package pe.edu.upeu.panaderia.ventas.pago.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.exception.PagoExcedeSaldoException;
import pe.edu.upeu.panaderia.exception.PagoYaAnuladoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.VentaAnuladaNoAceptaPagosException;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaRequest;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResponse;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResumenResponse;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPagoVenta;
import pe.edu.upeu.panaderia.ventas.pago.entity.MetodoPago;
import pe.edu.upeu.panaderia.ventas.pago.service.PagoVentaService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PagoVentaController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class PagoVentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PagoVentaService pagoVentaService;

    private PagoVentaResponse pagoResponse(EstadoPago estado) {
        return new PagoVentaResponse(50L, 1L, LocalDateTime.of(2026, 1, 1, 9, 0),
                MetodoPago.YAPE, new BigDecimal("20.00"), "OP-9", estado);
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Test
    @DisplayName("POST /ventas/{id}/pagos registra el pago y devuelve 201 con estado REGISTRADO")
    void registrarDevuelve201() throws Exception {
        when(pagoVentaService.registrar(eq(1L), any(PagoVentaRequest.class), any()))
                .thenReturn(pagoResponse(EstadoPago.REGISTRADO));

        mockMvc.perform(post("/api/v1/ventas/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PagoVentaRequest(MetodoPago.YAPE, new BigDecimal("20.00"), "OP-9"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.estado").value("REGISTRADO"))
                .andExpect(jsonPath("$.metodoPago").value("YAPE"));
    }

    @Test
    @DisplayName("POST de pago con monto cero devuelve 400")
    void registrarMontoCeroDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ventas/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PagoVentaRequest(MetodoPago.EFECTIVO, BigDecimal.ZERO, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("monto")));
    }

    @Test
    @DisplayName("POST de pago con método inválido devuelve 400")
    void registrarMetodoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ventas/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"CRYPTO\",\"monto\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST que excede el saldo devuelve 409")
    void registrarExcediendoSaldoDevuelve409() throws Exception {
        when(pagoVentaService.registrar(eq(1L), any(PagoVentaRequest.class), any()))
                .thenThrow(new PagoExcedeSaldoException(1L, new BigDecimal("5.00")));

        mockMvc.perform(post("/api/v1/ventas/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PagoVentaRequest(MetodoPago.TARJETA, new BigDecimal("20.00"), null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("saldo pendiente")));
    }

    @Test
    @DisplayName("POST sobre venta anulada devuelve 400")
    void registrarVentaAnuladaDevuelve400() throws Exception {
        when(pagoVentaService.registrar(eq(1L), any(PagoVentaRequest.class), any()))
                .thenThrow(new VentaAnuladaNoAceptaPagosException(1L));

        mockMvc.perform(post("/api/v1/ventas/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PagoVentaRequest(MetodoPago.EFECTIVO, new BigDecimal("5.00"), null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST sobre venta inexistente devuelve 404")
    void registrarVentaInexistenteDevuelve404() throws Exception {
        when(pagoVentaService.registrar(eq(99L), any(PagoVentaRequest.class), any()))
                .thenThrow(new ResourceNotFoundException("Venta", 99L));

        mockMvc.perform(post("/api/v1/ventas/99/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PagoVentaRequest(MetodoPago.EFECTIVO, new BigDecimal("5.00"), null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /ventas/{id}/pagos devuelve la lista")
    void listarPorVentaDevuelve200() throws Exception {
        when(pagoVentaService.listarPorVenta(eq(1L), any()))
                .thenReturn(List.of(pagoResponse(EstadoPago.REGISTRADO), pagoResponse(EstadoPago.ANULADO)));

        mockMvc.perform(get("/api/v1/ventas/1/pagos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].estado").value("ANULADO"));
    }

    @Test
    @DisplayName("GET /ventas/{id}/pagos/resumen devuelve total, saldo y situación")
    void resumenDevuelve200() throws Exception {
        when(pagoVentaService.resumen(eq(1L), any())).thenReturn(new PagoVentaResumenResponse(
                1L, new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("60.00"), EstadoPagoVenta.PARCIAL));

        mockMvc.perform(get("/api/v1/ventas/1/pagos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVenta").value(100.00))
                .andExpect(jsonPath("$.totalPagado").value(40.00))
                .andExpect(jsonPath("$.saldoPendiente").value(60.00))
                .andExpect(jsonPath("$.estadoPago").value("PARCIAL"));
    }

    @Test
    @DisplayName("GET /pagos-venta/{id} inexistente devuelve 404")
    void obtenerPagoInexistenteDevuelve404() throws Exception {
        when(pagoVentaService.obtener(eq(99L), any())).thenThrow(new ResourceNotFoundException("Pago", 99L));

        mockMvc.perform(get("/api/v1/pagos-venta/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /pagos-venta/{id}/anular devuelve el pago con estado ANULADO")
    void anularDevuelve200() throws Exception {
        when(pagoVentaService.anular(eq(50L), any())).thenReturn(pagoResponse(EstadoPago.ANULADO));

        mockMvc.perform(patch("/api/v1/pagos-venta/50/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }

    @Test
    @DisplayName("PATCH de un pago ya anulado devuelve 409")
    void anularYaAnuladoDevuelve409() throws Exception {
        when(pagoVentaService.anular(eq(50L), any())).thenThrow(new PagoYaAnuladoException(50L));

        mockMvc.perform(patch("/api/v1/pagos-venta/50/anular"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("ya está anulado")));
    }

    @Test
    @DisplayName("DELETE /pagos-venta/{id} está prohibido")
    void noExisteDeleteDePago() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/pagos-venta/50"))
                .andExpect(status().isForbidden());
    }

@Test
    @DisplayName("GET /ventas/99/pagos/resumen de una venta inexistente devuelve 404")
    void resumenVentaInexistenteDevuelve404() throws Exception {
        when(pagoVentaService.resumen(eq(99L), any())).thenThrow(new ResourceNotFoundException("Venta", 99L));

        mockMvc.perform(get("/api/v1/ventas/99/pagos/resumen"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /ventas/99/pagos de una venta inexistente devuelve 404")
    void listarVentaInexistenteDevuelve404() throws Exception {
        when(pagoVentaService.listarPorVenta(eq(99L), any())).thenThrow(new ResourceNotFoundException("Venta", 99L));

        mockMvc.perform(get("/api/v1/ventas/99/pagos"))
                .andExpect(status().isNotFound());
    }

    }