package pe.edu.upeu.panaderia;

import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;
import pe.edu.upeu.panaderia.ventas.venta.service.VentaService;

@SpringBootTest(properties = "app.security.permit-all=true")
@AutoConfigureMockMvc
class SecurityPermitAllTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private InsumoService insumoService;

    @MockitoBean
    private VentaService ventaService;

    @Test
    @DisplayName("permit-all=true: GET /api/v1/insumos es accesible sin JWT")
    void insumosAccesiblesSinJwt() throws Exception {
        when(insumoService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/insumos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("permit-all=true: una ruta antes protegida deja de responder 401")
    void rutaAntesProtegidaAccesibleSinJwt() throws Exception {
        when(ventaService.listar(null, null, null, null, null, null, null, "DESC", IdentidadJwt.anonima()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("permit-all=true: /actuator/health sigue siendo público")
    void healthSiguePublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
