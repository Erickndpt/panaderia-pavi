package pe.edu.upeu.panaderia.finanzas.movimiento.controller;

import java.math.BigDecimal;
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
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.ResumenFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasService;

@WebMvcTest(ReporteFinanzasController.class)
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class ReporteFinanzasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinanzasService finanzasService;

    @Test
    @DisplayName("GET /api/v1/reportes/finanzas responde 200 con el resumen de activos")
    void resumenDeberiaResponder200ConCincoCampos() throws Exception {
        when(finanzasService.resumen()).thenReturn(new ResumenFinancieroResponse(
                2L,
                new BigDecimal("60.00"),
                1L,
                new BigDecimal("17.00"),
                new BigDecimal("43.00")));

        mockMvc.perform(get("/api/v1/reportes/finanzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadIngresos").value(2))
                .andExpect(jsonPath("$.montoIngresos").value(60.00))
                .andExpect(jsonPath("$.cantidadEgresos").value(1))
                .andExpect(jsonPath("$.montoEgresos").value(17.00))
                .andExpect(jsonPath("$.saldo").value(43.00));
    }
}