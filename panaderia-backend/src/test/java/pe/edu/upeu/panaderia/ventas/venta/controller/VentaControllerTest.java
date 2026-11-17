package pe.edu.upeu.panaderia.ventas.venta.controller;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.exception.AccesoDenegadoException;
import pe.edu.upeu.panaderia.exception.ClienteInactivoException;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.VentaYaAnuladaException;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResumen;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;
import pe.edu.upeu.panaderia.ventas.reporte.controller.ReporteVentaController;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.reporte.service.ReporteVentaService;
import pe.edu.upeu.panaderia.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.DetalleVentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.service.VentaService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest({VentaController.class, ReporteVentaController.class})
@Import({pe.edu.upeu.panaderia.SecurityConfig.class, pe.edu.upeu.panaderia.security.SecurityTestSupport.class})
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VentaService ventaService;

    @MockitoBean
    private ReporteVentaService reporteVentaService;

    private DetalleVentaRequest detalleRequestValido() {
        return new DetalleVentaRequest(1L, 2);
    }

    private VentaRequest ventaRequestValida() {
        return new VentaRequest(9L, TipoVenta.LOCAL, null, List.of(detalleRequestValido()));
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
                List.of(new DetalleVentaResponse(1L, 1L, "Pan Integral", new BigDecimal("3.50"), 2, new BigDecimal("7.00"))));
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
                List.of(new DetalleVentaResponse(1L, 1L, "Pan Integral", new BigDecimal("3.50"), 2, new BigDecimal("7.00"))));
    }

    private ReporteVentaResponse reporteConDatos() {
        return new ReporteVentaResponse(
                2L,
                new BigDecimal("18.50"),
                1L,
                new BigDecimal("7.00"),
                1L,
                new BigDecimal("11.50"));
    }

    private String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    private IdentidadJwt identidadDefault() {
        return new IdentidadJwt("user", null, null, false);
    }

    private IdentidadJwt identidadVendedor(String id) {
        return new IdentidadJwt(id, null, null, true);
    }

    private IdentidadJwt identidadNoVendedor(String id) {
        return new IdentidadJwt(id, null, null, false);
    }

    private RequestPostProcessor jwtConRoles(String subject, String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return jwt()
                .jwt(builder -> builder
                        .subject(subject)
                        .claim("realm_access", Map.of("roles", List.of(roles))))
                .authorities(authorities);
    }

    @Test
    @DisplayName("GET /api/v1/ventas responde 200")
    void listarDeberiaResponder200() throws Exception {
        when(ventaService.listar(null, null, null, null, null, null, null, "DESC", identidadDefault()))
                .thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ventas?tipo=LOCAL responde 200")
    void listarPorTipoDeberiaResponder200() throws Exception {
        when(ventaService.listar(TipoVenta.LOCAL, null, null, null, null, null, null, "DESC", identidadDefault()))
                .thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas").param("tipo", "LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("LOCAL"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas con rango de fechas responde 200")
    void listarPorRangoDeFechasDeberiaResponder200() throws Exception {
        when(ventaService.listar(null,
                null,
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 30, 23, 59, 59),
                null,
                null,
                null,
                "DESC",
                identidadDefault())).thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas")
                        .param("fechaDesde", "2026-09-01T00:00:00")
                        .param("fechaHasta", "2026-09-30T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ventas con fechas inválidas responde 400")
    void listarConFechasInvalidasDeberiaResponder400() throws Exception {
        when(ventaService.listar(null,
                null,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 9, 1, 0, 0),
                null,
                null,
                null,
                "DESC",
                identidadDefault()))
                .thenThrow(new FiltroInvalidoException("fechaDesde no debe ser posterior a fechaHasta"));

        mockMvc.perform(get("/api/v1/ventas")
                        .param("fechaDesde", "2026-10-01T00:00:00")
                        .param("fechaHasta", "2026-09-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("fechaDesde no debe ser posterior a fechaHasta"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas con filtros estado y productoId responde 200")
    void listarPorEstadoYProductoDeberiaResponder200() throws Exception {
        when(ventaService.listar(null, EstadoVenta.ANULADA, null, null, null, 1L, null, "DESC", identidadDefault()))
                .thenReturn(List.of(ventaResponseAnulada()));

        mockMvc.perform(get("/api/v1/ventas")
                        .param("estado", "ANULADA")
                        .param("productoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ANULADA"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas con ordenFecha=ASC delega el ordenamiento correctamente")
    void listarConOrdenAscendenteDeberiaDelegarOrden() throws Exception {
        when(ventaService.listar(null, null, null, null, null, null, null, "ASC", identidadDefault()))
                .thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas").param("ordenFecha", "ASC"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ventas/resumen responde el mismo contenido que GET /api/v1/reportes/ventas")
    void resumenDeberiaResponderIgualQueReporteVentas() throws Exception {
        when(reporteVentaService.generar(any(), any())).thenReturn(reporteConDatos());

        String reporte = mockMvc.perform(get("/api/v1/reportes/ventas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String resumen = mockMvc.perform(get("/api/v1/ventas/resumen"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(resumen).isEqualTo(reporte);
    }

    @Test
    @DisplayName("PATCH /api/v1/ventas/{id}/anular responde 200 con la venta anulada")
    void anularDeberiaResponder200() throws Exception {
        when(ventaService.anular(1L)).thenReturn(ventaResponseAnulada());

        mockMvc.perform(patch("/api/v1/ventas/1/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    @Test
    @DisplayName("PATCH /api/v1/ventas/{id}/anular con venta inexistente responde 404")
    void anularVentaInexistenteDeberiaResponder404() throws Exception {
        when(ventaService.anular(99L)).thenThrow(new ResourceNotFoundException("Venta", 99L));

        mockMvc.perform(patch("/api/v1/ventas/99/anular"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/ventas/{id}/anular con venta ya anulada responde 409")
    void anularVentaYaAnuladaDeberiaResponder409() throws Exception {
        when(ventaService.anular(1L)).thenThrow(new VentaYaAnuladaException(1L));

        mockMvc.perform(patch("/api/v1/ventas/1/anular"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La venta con id 1 ya está anulada"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/{id} responde 200")
    void obtenerDeberiaResponder200() throws Exception {
        when(ventaService.obtener(1L, identidadDefault())).thenReturn(ventaResponseValida());

        mockMvc.perform(get("/api/v1/ventas/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.detalles[0].productoId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/ventas/{id} inexistente responde 404")
    void obtenerInexistenteDeberiaResponder404() throws Exception {
        when(ventaService.obtener(99L, identidadDefault())).thenThrow(new ResourceNotFoundException("Venta", 99L));

        mockMvc.perform(get("/api/v1/ventas/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/ventas responde 201 e incluye el cliente de la venta")
    void registrarDeberiaResponder201() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class), any())).thenReturn(ventaResponseValida());

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ventaRequestValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipo").value("LOCAL"))
                .andExpect(jsonPath("$.cliente.id").value(9))
                .andExpect(jsonPath("$.cliente.nombreRazonSocial").value("Panadería Pavi"))
                .andExpect(jsonPath("$.cliente.tipoDocumento").value("DNI"))
                .andExpect(jsonPath("$.cliente.numeroDocumento").value("87654321"))
                .andExpect(jsonPath("$.detalles[0].productoId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/ventas sin clienteId responde 400")
    void registrarSinClienteDeberiaResponder400() throws Exception {
        VentaRequest request = new VentaRequest(null, TipoVenta.LOCAL, null, List.of(detalleRequestValido()));

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ventas con cliente inexistente responde 404")
    void registrarConClienteInexistenteDeberiaResponder404() throws Exception {
        VentaRequest request = new VentaRequest(99L, TipoVenta.LOCAL, null, List.of(detalleRequestValido()));
        when(ventaService.registrar(any(VentaRequest.class), any()))
                .thenThrow(new ResourceNotFoundException("Cliente", 99L));

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/ventas con cliente inactivo responde 409")
    void registrarConClienteInactivoDeberiaResponder409() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class), any()))
                .thenThrow(new ClienteInactivoException(9L));

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ventaRequestValida())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El cliente con id 9 está inactivo"));
    }

    @Test
    @DisplayName("GET /api/v1/ventas con clienteId delega el filtro al servicio")
    void listarPorClienteDeberiaResponder200() throws Exception {
        when(ventaService.listar(null, null, null, null, null, null, 9L, "DESC", identidadDefault()))
                .thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas").param("clienteId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cliente.id").value(9));
    }

    @Test
    @DisplayName("POST /api/v1/ventas usa el sub del JWT y VentaRequest no acepta vendedorId")
    void registrarDeberiaTomarVendedorIdDelJwt() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class), any())).thenReturn(ventaResponseValida());

        mockMvc.perform(post("/api/v1/ventas")
                        .with(jwtConRoles("vendedor-sub-1", "offline_access", "uma_authorization", "VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ventaRequestValida())))
                .andExpect(status().isCreated());

        verify(ventaService).registrar(any(VentaRequest.class), eq(identidadVendedor("vendedor-sub-1")));
        assertThat(Arrays.stream(VentaRequest.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("vendedorId");
    }

    @Test
    @DisplayName("VENDEDOR lista solo sus ventas: se pasa su sub al servicio")
    void listarComoVendedorDeberiaPasarSuSubject() throws Exception {
        when(ventaService.listar(null, null, null, null, null, null, null, "DESC", identidadVendedor("vendedor-sub-1")))
                .thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas")
                        .with(jwtConRoles("vendedor-sub-1", "offline_access", "uma_authorization", "VENDEDOR")))
                .andExpect(status().isOk());

        verify(ventaService).listar(
                null, null, null, null, null, null, null, "DESC", identidadVendedor("vendedor-sub-1"));
    }

    @Test
    @DisplayName("SUPERVISOR y ADMIN listan todas las ventas sin filtro por vendedor")
    void listarComoSupervisorYAdminNoDeberiaFiltrarPorVendedor() throws Exception {
        when(ventaService.listar(null, null, null, null, null, null, null, "DESC",
                identidadNoVendedor("supervisor-sub")))
                .thenReturn(List.of(ventaResponseValida()));
        when(ventaService.listar(null, null, null, null, null, null, null, "DESC",
                identidadNoVendedor("admin-sub")))
                .thenReturn(List.of(ventaResponseValida()));

        mockMvc.perform(get("/api/v1/ventas")
                        .with(jwtConRoles("supervisor-sub", "offline_access", "SUPERVISOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ventas")
                        .with(jwtConRoles("admin-sub", "offline_access", "ADMIN")))
                .andExpect(status().isOk());

        verify(ventaService).listar(null, null, null, null, null, null, null, "DESC",
                identidadNoVendedor("supervisor-sub"));
        verify(ventaService).listar(null, null, null, null, null, null, null, "DESC",
                identidadNoVendedor("admin-sub"));
    }

    @Test
    @DisplayName("VENDEDOR recibe 403 al obtener una venta ajena")
    void obtenerVentaAjenaComoVendedorDeberiaResponder403() throws Exception {
        when(ventaService.obtener(1L, identidadVendedor("vendedor-sub-2")))
                .thenThrow(new AccesoDenegadoException(
                        "No tiene permiso para acceder a la venta con id 1"));

        mockMvc.perform(get("/api/v1/ventas/1")
                        .with(jwtConRoles("vendedor-sub-2", "offline_access", "uma_authorization", "VENDEDOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "No tiene permiso para acceder a la venta con id 1"));
    }

    @Test
    @DisplayName("SUPERVISOR y ADMIN pueden obtener la venta de cualquier vendedor")
    void obtenerVentaAjenaComoSupervisorYAdminDeberiaPermitir() throws Exception {
        when(ventaService.obtener(1L, identidadNoVendedor("supervisor-sub")))
                .thenReturn(ventaResponseValida());
        when(ventaService.obtener(1L, identidadNoVendedor("admin-sub")))
                .thenReturn(ventaResponseValida());

        mockMvc.perform(get("/api/v1/ventas/1")
                        .with(jwtConRoles("supervisor-sub", "offline_access", "SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(get("/api/v1/ventas/1")
                        .with(jwtConRoles("admin-sub", "offline_access", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(ventaService).obtener(1L, identidadNoVendedor("supervisor-sub"));
        verify(ventaService).obtener(1L, identidadNoVendedor("admin-sub"));
    }

    @Test
    @DisplayName("POST /api/v1/ventas sin detalles responde 400")
    void registrarSinDetallesDeberiaResponder400() throws Exception {
        VentaRequest request = new VentaRequest(9L, TipoVenta.LOCAL, null, List.of());

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Al enviar X-Trace-ID, la respuesta devuelve el mismo valor")
    void trazaEnviadaSeDevuelveEnLaRespuesta() throws Exception {
        mockMvc.perform(get("/api/v1/ventas").header("X-Trace-ID", "traza-venta-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", "traza-venta-1"));
    }

    @Test
    @DisplayName("Sin X-Trace-ID, la respuesta contiene un id de traza no vacío")
    void trazaGeneradaCuandoNoSeEnviaHeader() throws Exception {
        mockMvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", not(emptyString())));
    }
}