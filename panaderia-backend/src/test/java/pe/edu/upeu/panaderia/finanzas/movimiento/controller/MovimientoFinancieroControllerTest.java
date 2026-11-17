package pe.edu.upeu.panaderia.finanzas.movimiento.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.MovimientoFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.EstadoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.OrigenMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.TipoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasService;

@WebMvcTest(MovimientoFinancieroController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class MovimientoFinancieroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinanzasService finanzasService;

    private MovimientoFinancieroResponse ingreso() {
        return new MovimientoFinancieroResponse(
                1L,
                LocalDateTime.of(2026, 9, 14, 10, 0),
                TipoMovimiento.INGRESO,
                OrigenMovimiento.VENTA,
                1L,
                "Ingreso por venta 1 (LOCAL)",
                new BigDecimal("25.00"),
                EstadoMovimiento.ACTIVO);
    }

    @Test
    @DisplayName("GET /api/v1/movimientos-financieros responde 200 con el listado")
    void listarDeberiaResponder200ConMovimientos() throws Exception {
        when(finanzasService.listar()).thenReturn(List.of(ingreso()));

        mockMvc.perform(get("/api/v1/movimientos-financieros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tipo").value("INGRESO"))
                .andExpect(jsonPath("$[0].origen").value("VENTA"))
                .andExpect(jsonPath("$[0].origenId").value(1))
                .andExpect(jsonPath("$[0].concepto").value("Ingreso por venta 1 (LOCAL)"))
                .andExpect(jsonPath("$[0].monto").value(25.00))
                .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("GET /api/v1/movimientos-financieros/{id} con un id existente responde 200")
    void obtenerExistenteDeberiaResponder200() throws Exception {
        when(finanzasService.obtener(1L)).thenReturn(ingreso());

        mockMvc.perform(get("/api/v1/movimientos-financieros/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.monto").value(25.00))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("GET /api/v1/movimientos-financieros/{id} con un id inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(finanzasService.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Movimiento financiero", 99L));

        mockMvc.perform(get("/api/v1/movimientos-financieros/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Movimiento financiero con id 99 no encontrado"));
    }
}