package pe.edu.upeu.panaderia.ventas.reporte.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.SecurityConfig;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.security.SecurityTestSupport;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.reporte.service.ReporteVentaService;

@WebMvcTest(ReporteVentaController.class)
@Import({SecurityConfig.class, SecurityTestSupport.class})
class ReporteVentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentaService reporteVentaService;

    private ReporteVentaResponse reporteConDatos() {
        return new ReporteVentaResponse(
                2L,
                new BigDecimal("18.50"),
                1L,
                new BigDecimal("7.00"),
                1L,
                new BigDecimal("11.50"));
    }

    private ReporteVentaResponse reporteSinDatos() {
        return new ReporteVentaResponse(
                0L,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /api/v1/reportes/ventas con datos responde 200 con los seis campos")
    void reporteConDatosDeberiaResponder200ConSeisCampos() throws Exception {
        when(reporteVentaService.generar(any(), any())).thenReturn(reporteConDatos());

        mockMvc.perform(get("/api/v1/reportes/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadVentas").value(2))
                .andExpect(jsonPath("$.montoTotal").value(18.50))
                .andExpect(jsonPath("$.cantidadLocales").value(1))
                .andExpect(jsonPath("$.montoLocal").value(7.00))
                .andExpect(jsonPath("$.cantidadExportaciones").value(1))
                .andExpect(jsonPath("$.montoExportacion").value(11.50));
    }

    @Test
    @DisplayName("GET /api/v1/reportes/ventas sin datos responde 200 con valores en cero")
    void reporteVacioDeberiaResponder200ConCeros() throws Exception {
        when(reporteVentaService.generar(any(), any())).thenReturn(reporteSinDatos());

        mockMvc.perform(get("/api/v1/reportes/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadVentas").value(0))
                .andExpect(jsonPath("$.montoTotal").value(0))
                .andExpect(jsonPath("$.cantidadLocales").value(0))
                .andExpect(jsonPath("$.montoLocal").value(0))
                .andExpect(jsonPath("$.cantidadExportaciones").value(0))
                .andExpect(jsonPath("$.montoExportacion").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/reportes/ventas con fechas inválidas responde 400")
    void reporteConFechasInvalidasDeberiaResponder400() throws Exception {
        when(reporteVentaService.generar(
                eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0))))
                .thenThrow(new FiltroInvalidoException("fechaDesde no debe ser posterior a fechaHasta"));

        mockMvc.perform(get("/api/v1/reportes/ventas")
                        .param("fechaDesde", "2026-10-01T00:00:00")
                        .param("fechaHasta", "2026-09-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fechaDesde no debe ser posterior a fechaHasta"));
    }
}