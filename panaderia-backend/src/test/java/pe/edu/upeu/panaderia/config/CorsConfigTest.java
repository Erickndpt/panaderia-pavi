package pe.edu.upeu.panaderia.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.panaderia.administracion.producto.controller.ProductoController;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;

@WebMvcTest(ProductoController.class)
@Import({CorsConfig.class, pe.edu.upeu.panaderia.SecurityConfig.class,
        pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
@TestPropertySource(properties = "app.security.permit-all=false")
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @Test
    @DisplayName("Preflight OPTIONS desde Angular permite el origen y expone X-Trace-ID")
    void preflightDeberiaPermitirOrigenAngular() throws Exception {
        mockMvc.perform(options("/api/v1/productos")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Trace-ID")));
    }
}