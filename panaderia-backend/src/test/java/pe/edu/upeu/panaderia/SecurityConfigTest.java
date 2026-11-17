package pe.edu.upeu.panaderia;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResponse;
import pe.edu.upeu.panaderia.administracion.categoria.service.CategoriaService;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResumen;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaRequest;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResponse;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.MetodoPago;
import pe.edu.upeu.panaderia.ventas.pago.service.PagoVentaService;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.reporte.service.ReporteVentaService;
import pe.edu.upeu.panaderia.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.service.VentaService;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.security.permit-all=false")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private VentaService ventaService;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private ReporteVentaService reporteVentaService;

    @MockitoBean
    private PagoVentaService pagoVentaService;

    @MockitoBean
    private ExistenciaService existenciaService;

    private RequestPostProcessor jwtWithRoles(String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return jwt().authorities(authorities);
    }

    private VentaRequest ventaRequestValida() {
        return new VentaRequest(9L, TipoVenta.LOCAL, null, List.of(new DetalleVentaRequest(1L, 2)));
    }

    private VentaResponse ventaResponseValida() {
        return new VentaResponse(
                1L,
                LocalDateTime.of(2026, 9, 14, 10, 30),
                TipoVenta.LOCAL,
                null,
                new ClienteResumen(9L, TipoDocumento.DNI, "87654321", "Panadería Pavi"),
                EstadoVenta.REGISTRADA,
                new BigDecimal("7.00"),
                null,
                List.of());
    }

    private VentaResponse ventaResponseAnulada() {
        return new VentaResponse(
                1L,
                LocalDateTime.of(2026, 9, 14, 10, 30),
                TipoVenta.LOCAL,
                null,
                new ClienteResumen(9L, TipoDocumento.DNI, "87654321", "Panadería Pavi"),
                EstadoVenta.ANULADA,
                new BigDecimal("7.00"),
                null,
                List.of());
    }

    @Test
    @DisplayName("(a) Swagger y /actuator/health son públicos")
    void endpointsPublicos() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        int swaggerStatus = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn().getResponse().getStatus();
        assertThat(swaggerStatus).isBetween(200, 399);
    }

    @Test
    @DisplayName("(b) Una ruta protegida sin token responde 401")
    void rutaProtegidaSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("(c) VENDEDOR puede registrar una venta (POST /api/v1/ventas)")
    void vendedorPuedeRegistrarVenta() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class), any())).thenReturn(ventaResponseValida());

        mockMvc.perform(post("/api/v1/ventas")
                        .with(jwtWithRoles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaRequestValida())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("(d) VENDEDOR no puede anular una venta (403)")
    void vendedorNoPuedeAnularVenta() throws Exception {
        mockMvc.perform(patch("/api/v1/ventas/1/anular").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("(e) SUPERVISOR puede anular una venta (200)")
    void supervisorPuedeAnularVenta() throws Exception {
        when(ventaService.anular(1L)).thenReturn(ventaResponseAnulada());

        mockMvc.perform(patch("/api/v1/ventas/1/anular").with(jwtWithRoles("SUPERVISOR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("(f) ADMIN puede modificar productos y categorías")
    void adminPuedeModificarCatalogo() throws Exception {
        when(categoriaService.crear(any())).thenReturn(CategoriaResponse.builder()
                .id(1L)
                .nombre("Panificación")
                .build());

        mockMvc.perform(post("/api/v1/categorias")
                        .with(jwtWithRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Panificación\",\"descripcion\":\"Productos\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/productos/1").with(jwtWithRoles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("(g) VENDEDOR no puede modificar productos ni categorías (403)")
    void vendedorNoPuedeModificarCatalogo() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .with(jwtWithRoles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Panificación\",\"descripcion\":\"Productos\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/productos/1").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("(g-bis) VENDEDOR sí puede consultar productos y categorías (200)")
    void vendedorPuedeLeerCatalogo() throws Exception {
        when(categoriaService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categorias").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ventas/resumen está prohibido para VENDEDOR")
    void resumenRestringidoParaVendedor() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/resumen").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPERVISOR y ADMIN conservan el acceso al resumen de ventas")
    void supervisorYAdminPuedenVerResumen() throws Exception {
        when(reporteVentaService.generar(any(), any())).thenReturn(
                new ReporteVentaResponse(0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO));

        mockMvc.perform(get("/api/v1/ventas/resumen").with(jwtWithRoles("SUPERVISOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ventas/resumen").with(jwtWithRoles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN conserva el permiso para anular ventas")
    void adminPuedeAnularVenta() throws Exception {
        when(ventaService.anular(1L)).thenReturn(ventaResponseAnulada());

        mockMvc.perform(patch("/api/v1/ventas/1/anular").with(jwtWithRoles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VENDEDOR solo accede a Ventas y a las existencias necesarias para vender")
    void vendedorSoloAccedeAVentasYExistencias() throws Exception {
        when(existenciaService.listar(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/existencias").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/insumos").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compras").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/proveedores").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ordenes-produccion").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/movimientos-financieros").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes/ventas").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPERVISOR consulta pero no modifica inventario, compras, producción ni finanzas")
    void supervisorNoModificaOtrosModulos() throws Exception {
        mockMvc.perform(post("/api/v1/insumos")
                        .with(jwtWithRoles("SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/compras")
                        .with(jwtWithRoles("SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/proveedores")
                        .with(jwtWithRoles("SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/ordenes-produccion")
                        .with(jwtWithRoles("SUPERVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/compras/1/anular").with(jwtWithRoles("SUPERVISOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN no puede registrar ventas directamente (403)")
    void adminNoPuedeRegistrarVenta() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .with(jwtWithRoles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaRequestValida())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GERENTE_GENERAL reúne gestión administrativa, supervisión y registro de ventas")
    void gerenteGeneralTieneAccesoTotal() throws Exception {
        when(categoriaService.crear(any())).thenReturn(CategoriaResponse.builder()
                .id(1L)
                .nombre("Panificación")
                .build());
        when(ventaService.registrar(any(VentaRequest.class), any())).thenReturn(ventaResponseValida());
        when(ventaService.anular(1L)).thenReturn(ventaResponseAnulada());
        when(reporteVentaService.generar(any(), any())).thenReturn(
                new ReporteVentaResponse(0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO));

        mockMvc.perform(post("/api/v1/categorias")
                        .with(jwtWithRoles("GERENTE_GENERAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Panificación\",\"descripcion\":\"Productos\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/ventas")
                        .with(jwtWithRoles("GERENTE_GENERAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaRequestValida())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ventas/resumen").with(jwtWithRoles("GERENTE_GENERAL")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/ventas/1/anular").with(jwtWithRoles("GERENTE_GENERAL")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VENDEDOR registra pagos de su venta pero no puede anularlos")
    void vendedorRegistraPagosPeroNoLosAnula() throws Exception {
        when(pagoVentaService.registrar(eq(1L), any(PagoVentaRequest.class), any()))
                .thenReturn(new PagoVentaResponse(50L, 1L, LocalDateTime.now(),
                        MetodoPago.YAPE, new BigDecimal("20.00"), null, EstadoPago.REGISTRADO));

        mockMvc.perform(post("/api/v1/ventas/1/pagos")
                        .with(jwtWithRoles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"YAPE\",\"monto\":20.00}"))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/pagos-venta/50/anular").with(jwtWithRoles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("(h) realm_access.roles se convierte en autoridades ROLE_*")
    void realmAccessRolesSeConvierte() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", List.of("VENDEDOR", "SUPERVISOR")))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_VENDEDOR", "ROLE_SUPERVISOR");
    }

    @Test
    @DisplayName("(h) Token sin realm_access no otorga autoridades y no falla")
    void tokenSinRealmAccessNoTieneAutoridades() {
        Jwt sinRealmAccess = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        Jwt realmAccessInvalido = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", "no-es-un-mapa")
                .build();

        assertThat(jwtAuthenticationConverter.convert(sinRealmAccess).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .noneMatch(authority -> authority.startsWith("ROLE_"));
        assertThat(jwtAuthenticationConverter.convert(realmAccessInvalido).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .noneMatch(authority -> authority.startsWith("ROLE_"));
    }
}
