package pe.edu.upeu.panaderia.ventas.venta.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResumen;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.exception.AccesoDenegadoException;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.exception.ClienteInactivoException;
import pe.edu.upeu.panaderia.exception.MovimientoDuplicadoException;
import pe.edu.upeu.panaderia.exception.MovimientoInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.StockInsuficienteException;
import pe.edu.upeu.panaderia.exception.VentaConPagosRegistradosException;
import pe.edu.upeu.panaderia.exception.VentaInvalidaException;
import pe.edu.upeu.panaderia.exception.VentaYaAnuladaException;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasVentaService;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;
import pe.edu.upeu.panaderia.ventas.AutorizacionVenta;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResumen;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;
import pe.edu.upeu.panaderia.ventas.cliente.service.ClienteService;
import pe.edu.upeu.panaderia.ventas.pago.service.PagoVentaService;
import pe.edu.upeu.panaderia.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.DetalleVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;
import pe.edu.upeu.panaderia.ventas.venta.mapper.VentaMapper;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private VentaMapper ventaMapper;

    @Mock
    private ProductoService productoService;

    @Mock
    private ExistenciaService existenciaService;

    @Mock
    private FinanzasVentaService finanzasService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private PagoVentaService pagoVentaService;

    @Spy
    private AutorizacionVenta autorizacionVenta;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private IdentidadJwt vendedor(String id) {
        return new IdentidadJwt(id, id, "Vendedor " + id, true);
    }

    private IdentidadJwt supervisor() {
        return new IdentidadJwt("supervisor-1", "supervisor", "Supervisor Uno", false);
    }

    private Cliente clienteActivo() {
        Cliente cliente = new Cliente();
        cliente.setId(9L);
        return cliente;
    }

    private ProductoResponse productoDe(String nombre, String precio) {
        return ProductoResponse.builder()
                .id(1L)
                .nombre(nombre)
                .precio(new BigDecimal(precio))
                .categoria(new CategoriaResumen(1L, "Panes"))
                .build();
    }

    @Test
    @DisplayName("Venta LOCAL exitosa: descuenta stock, calcula total y guarda una vez")
    void registrarLocalDeberiaDescontarCalcularYGuardar() {
        when(clienteService.obtenerActivo(9L)).thenReturn(clienteActivo());
        when(productoService.obtener(1L))
                .thenReturn(productoDe("Pan Integral", "3.50"));
        when(ventaRepository.save(any(Venta.class)))
                .thenAnswer(invocation -> {
                    Venta venta = invocation.getArgument(0);
                    venta.setId(1L);
                    return venta;
                });
        VentaRequest request = new VentaRequest(
                9L, TipoVenta.LOCAL, null, List.of(new DetalleVentaRequest(1L, 2)));

        ventaService.registrar(request, vendedor("vendedor-1"));

        verify(existenciaService).descontar(1L, new BigDecimal("2"));
        verify(finanzasService).registrarIngresoVenta(
                eq(1L), any(LocalDateTime.class), eq("LOCAL"), eq(new BigDecimal("7.00")));
        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());

        Venta venta = captor.getValue();
        assertThat(venta.getCliente().getId()).isEqualTo(9L);
        assertThat(venta.getTipo()).isEqualTo(TipoVenta.LOCAL);
        assertThat(venta.getPaisDestino()).isNull();
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);
        assertThat(venta.getTotal()).isEqualByComparingTo(new BigDecimal("7.00"));
        assertThat(venta.getVendedorId()).isEqualTo("vendedor-1");
        assertThat(venta.getVendedorUsername()).isEqualTo("vendedor-1");
        assertThat(venta.getVendedorNombre()).isEqualTo("Vendedor vendedor-1");
        assertThat(venta.getDetalles()).hasSize(1);
        assertThat(venta.getDetalles().get(0).getNombreProducto()).isEqualTo("Pan Integral");
        assertThat(venta.getDetalles().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("7.00"));
    }

    @Test
    @DisplayName("Cliente inexistente al registrar propaga 404 y no descuenta stock")
    void registrarConClienteInexistenteDeberiaPropagarSinGuardar() {
        when(clienteService.obtenerActivo(99L))
                .thenThrow(new ResourceNotFoundException("Cliente", 99L));
        VentaRequest request = new VentaRequest(
                99L, TipoVenta.LOCAL, null, List.of(new DetalleVentaRequest(1L, 2)));

        assertThatThrownBy(() -> ventaService.registrar(request, vendedor("vendedor-1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente")
                .hasMessageContaining("99");

        verify(ventaRepository, never()).save(any(Venta.class));
        verify(existenciaService, never()).descontar(any(), any());
    }

    @Test
    @DisplayName("Cliente inactivo al registrar propaga 409 y no descuenta stock")
    void registrarConClienteInactivoDeberiaPropagarSinGuardar() {
        when(clienteService.obtenerActivo(9L)).thenThrow(new ClienteInactivoException(9L));
        VentaRequest request = new VentaRequest(
                9L, TipoVenta.LOCAL, null, List.of(new DetalleVentaRequest(1L, 2)));

        assertThatThrownBy(() -> ventaService.registrar(request, vendedor("vendedor-1")))
                .isInstanceOf(ClienteInactivoException.class)
                .hasMessageContaining("inactivo");

        verify(ventaRepository, never()).save(any(Venta.class));
        verify(existenciaService, never()).descontar(any(), any());
    }

    @Test
    @DisplayName("Venta EXPORTACION sin país lanza VentaInvalidaException y no guarda")
    void registrarExportacionSinPaisDeberiaLanzarExcepcion() {
        VentaRequest request = new VentaRequest(
                9L, TipoVenta.EXPORTACION, null, List.of(new DetalleVentaRequest(1L, 2)));

        assertThatThrownBy(() -> ventaService.registrar(request, vendedor("vendedor-1")))
                .isInstanceOf(VentaInvalidaException.class)
                .hasMessageContaining("país");

        verify(ventaRepository, never()).save(any(Venta.class));
        verify(existenciaService, never()).descontar(any(), any());
    }

    @Test
    @DisplayName("Fallo por stock en el segundo detalle propaga 409 y no guarda la venta")
    void registrarConFalloDeStockEnSegundoDetalleDeberiaPropagarSinGuardar() {
        when(clienteService.obtenerActivo(9L)).thenReturn(clienteActivo());
        when(productoService.obtener(1L)).thenReturn(productoDe("Pan Integral", "3.50"));
        when(productoService.obtener(2L)).thenReturn(productoDe("Café", "5.00"));
        org.mockito.Mockito.doNothing().when(existenciaService).descontar(1L, new BigDecimal("2"));
        org.mockito.Mockito.doThrow(new StockInsuficienteException(2L, new BigDecimal("5"), new BigDecimal("3")))
                .when(existenciaService).descontar(eq(2L), eq(new BigDecimal("5")));
        VentaRequest request = new VentaRequest(
                9L,
                TipoVenta.LOCAL,
                null,
                List.of(new DetalleVentaRequest(1L, 2), new DetalleVentaRequest(2L, 5)));

        assertThatThrownBy(() -> ventaService.registrar(request, vendedor("vendedor-1")))
                .isInstanceOf(StockInsuficienteException.class);

        verify(existenciaService).descontar(1L, new BigDecimal("2"));
        verify(ventaRepository, never()).save(any(Venta.class));
        // El rollback de la primera actualización de stock (producto 1) lo realiza
        // Spring al abortar la transacción mediante @Transactional.
    }

    @Test
    @DisplayName("Producto inexistente propaga ResourceNotFoundException y no guarda")
    void registrarConProductoInexistenteDeberiaPropagarSinGuardar() {
        when(clienteService.obtenerActivo(9L)).thenReturn(clienteActivo());
        when(productoService.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Producto", 99L));
        VentaRequest request = new VentaRequest(
                9L, TipoVenta.LOCAL, null, List.of(new DetalleVentaRequest(99L, 2)));

        assertThatThrownBy(() -> ventaService.registrar(request, vendedor("vendedor-1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(ventaRepository, never()).save(any(Venta.class));
        verify(existenciaService, never()).descontar(any(), any());
    }

    @Test
    @DisplayName("Rango de fechas inválido en listar lanza FiltroInvalidoException")
    void listarConFechasInvalidasDeberiaLanzarExcepcion() {
        assertThatThrownBy(() -> ventaService.listar(
                null,
                null,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 9, 1, 0, 0),
                null,
                null,
                null,
                "DESC",
                null))
                .isInstanceOf(FiltroInvalidoException.class)
                .hasMessageContaining("fechaDesde");
    }

    @Test
    @DisplayName("listar con vendedorId conserva los filtros existentes y usa Specification")
    void listarConVendedorYFiltrosDeberiaUsarSpecification() {
        when(ventaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        List<VentaResponse> response = ventaService.listar(
                TipoVenta.LOCAL,
                EstadoVenta.REGISTRADA,
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 30, 23, 59),
                "Perú",
                1L,
                9L,
                "ASC",
                vendedor("vendedor-1"));

        assertThat(response).isEmpty();
        ArgumentCaptor<Specification<Venta>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(ventaRepository).findAll(captor.capture(), any(Sort.class));
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Un vendedor puede obtener su propia venta")
    void obtenerVentaPropiaDeberiaPermitir() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        venta.setVendedorId("vendedor-1");
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaMapper.toResponse(venta)).thenReturn(ventaResponseAnulada());

        VentaResponse response = ventaService.obtener(1L, vendedor("vendedor-1"));

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Un vendedor no puede obtener la venta de otro vendedor")
    void obtenerVentaAjenaDeberiaLanzarAccesoDenegado() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        venta.setVendedorId("vendedor-1");
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.obtener(1L, vendedor("vendedor-2")))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(ventaMapper, never()).toResponse(any(Venta.class));
    }

    @Test
    @DisplayName("Un vendedor no puede obtener una venta histórica sin propietario")
    void obtenerVentaHistoricaSinVendedorDeberiaLanzarAccesoDenegado() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        venta.setVendedorId(null);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.obtener(1L, vendedor("vendedor-1")))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(ventaMapper, never()).toResponse(any(Venta.class));
    }

    @Test
    @DisplayName("Sin vendedorId (SUPERVISOR/ADMIN) se puede obtener cualquier venta")
    void obtenerSinVendedorIdDeberiaPermitirCualquierVenta() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        venta.setVendedorId("vendedor-1");
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaMapper.toResponse(venta)).thenReturn(ventaResponseAnulada());

        VentaResponse response = ventaService.obtener(1L, supervisor());

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Anular venta REGISTRADA repone todos los detalles, cambia a ANULADA y no elimina la venta")
    void anularVentaRegistradaDeberiaReponerCambiarEstadoYNoEliminar() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        when(ventaMapper.toResponse(venta)).thenReturn(ventaResponseAnulada());

        VentaResponse response = ventaService.anular(1L);

        assertThat(response.estado()).isEqualTo(EstadoVenta.ANULADA);
        verify(existenciaService).reponer(1L, new BigDecimal("2"));
        verify(existenciaService).reponer(2L, new BigDecimal("5"));
        verify(finanzasService).anularIngresoVenta(1L);
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
        assertThat(venta.getDetalles()).hasSize(2);
        verify(ventaRepository, never()).delete(any(Venta.class));
    }

    @Test
    @DisplayName("Anular venta con pagos REGISTRADO lanza VentaConPagosRegistradosException y no repone stock")
    void anularVentaConPagosRegistradosDeberiaLanzarSinReponer() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaService.tienePagosRegistrados(1L)).thenReturn(true);

        assertThatThrownBy(() -> ventaService.anular(1L))
                .isInstanceOf(VentaConPagosRegistradosException.class)
                .hasMessageContaining("no puede anularse");

        verify(existenciaService, never()).reponer(any(), any());
        verify(finanzasService, never()).anularIngresoVenta(any());
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);
    }

    @Test
    @DisplayName("Anular venta ya ANULADA lanza VentaYaAnuladaException y no repone")
    void anularVentaAnuladaDeberiaLanzarSinReponer() {
        Venta venta = ventaConDosDetalles(EstadoVenta.ANULADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.anular(1L))
                .isInstanceOf(VentaYaAnuladaException.class)
                .hasMessageContaining("ya está anulada");

        verify(existenciaService, never()).reponer(any(), any());
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
    }

    @Test
    @DisplayName("Fallo al reponer un detalle propaga la excepción y no cambia el estado")
    void anularConFalloAlReponerDeberiaPropagarSinCambiarEstado() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Existencia del producto", 2L))
                .when(existenciaService).reponer(eq(1L), eq(new BigDecimal("2")));

        assertThatThrownBy(() -> ventaService.anular(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("2");

        verify(existenciaService).reponer(1L, new BigDecimal("2"));
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);
    }

    @Test
    @DisplayName("Anular venta inexistente lanza ResourceNotFoundException")
    void anularVentaInexistenteDeberiaLanzarExcepcion() {
        when(ventaRepository.buscarConBloqueoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.anular(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(existenciaService, never()).reponer(any(), any());
    }

    @Test
    @DisplayName("Fallo al registrar el ingreso financiero propaga la excepción y revierte la venta")
    void registrarConFalloDelMovimientoFinancieroDeberiaPropagar() {
        when(clienteService.obtenerActivo(9L)).thenReturn(clienteActivo());
        when(productoService.obtener(1L)).thenReturn(productoDe("Pan Integral", "3.50"));
        when(ventaRepository.save(any(Venta.class)))
                .thenAnswer(invocation -> {
                    Venta venta = invocation.getArgument(0);
                    venta.setId(1L);
                    return venta;
                });
        org.mockito.Mockito.doThrow(new MovimientoDuplicadoException("venta", 1L))
                .when(finanzasService).registrarIngresoVenta(
                        eq(1L), any(LocalDateTime.class), any(String.class), any(BigDecimal.class));
        VentaRequest request = new VentaRequest(
                9L, TipoVenta.LOCAL, null, List.of(new DetalleVentaRequest(1L, 2)));

        assertThatThrownBy(() -> ventaService.registrar(request, vendedor("vendedor-1")))
                .isInstanceOf(MovimientoDuplicadoException.class)
                .hasMessageContaining("venta");

        // La venta se guardó en memoria, pero el rollback de la transacción
        // (@Transactional) revierte la venta, el descuento de stock y el
        // movimiento financiero al fallar este último.
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Fallo al anular el ingreso financiero propaga la excepción y la transacción revierte la anulación")
    void anularConFalloAlAnularElIngresoDeberiaPropagarSinCambiarEstado() {
        Venta venta = ventaConDosDetalles(EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        org.mockito.Mockito.doThrow(new MovimientoInvalidoException("El ingreso de la venta 1 ya está anulado"))
                .when(finanzasService).anularIngresoVenta(1L);

        assertThatThrownBy(() -> ventaService.anular(1L))
                .isInstanceOf(MovimientoInvalidoException.class)
                .hasMessageContaining("ya está anulado");

        verify(existenciaService).reponer(1L, new BigDecimal("2"));
        verify(existenciaService).reponer(2L, new BigDecimal("5"));
        // El estado quedó marcado en memoria, pero el rollback de la
        // transacción (@Transactional) revierte la anulación al fallar la
        // anulación del ingreso financiero.
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
    }

    private Venta ventaConDosDetalles(EstadoVenta estado) {
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstado(estado);
        venta.setTotal(new BigDecimal("23.00"));
        DetalleVenta primero = new DetalleVenta();
        primero.setIdProducto(1L);
        primero.setCantidad(2);
        DetalleVenta segundo = new DetalleVenta();
        segundo.setIdProducto(2L);
        segundo.setCantidad(5);
        venta.agregarDetalle(primero);
        venta.agregarDetalle(segundo);
        return venta;
    }

    private VentaResponse ventaResponseAnulada() {
        return new VentaResponse(
                1L,
                LocalDateTime.of(2026, 9, 14, 10, 30),
                TipoVenta.LOCAL,
                null,
                new ClienteResumen(9L, TipoDocumento.DNI, "87654321", "Panadería Pavi"),
                EstadoVenta.ANULADA,
                new BigDecimal("23.00"),
                null,
                List.of());
    }
}
