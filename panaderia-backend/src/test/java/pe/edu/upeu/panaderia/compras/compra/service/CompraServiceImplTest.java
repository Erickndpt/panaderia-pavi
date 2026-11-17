package pe.edu.upeu.panaderia.compras.compra.service;

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
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraRequest;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraResponse;
import pe.edu.upeu.panaderia.compras.compra.dto.DetalleCompraRequest;
import pe.edu.upeu.panaderia.compras.compra.entity.Compra;
import pe.edu.upeu.panaderia.compras.compra.entity.DetalleCompra;
import pe.edu.upeu.panaderia.compras.compra.entity.EstadoCompra;
import pe.edu.upeu.panaderia.compras.compra.mapper.CompraMapper;
import pe.edu.upeu.panaderia.compras.compra.repository.CompraRepository;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResumen;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;
import pe.edu.upeu.panaderia.compras.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.panaderia.exception.CompraInvalidaException;
import pe.edu.upeu.panaderia.exception.CompraYaAnuladaException;
import pe.edu.upeu.panaderia.exception.InsumoStockInsuficienteException;
import pe.edu.upeu.panaderia.exception.MovimientoDuplicadoException;
import pe.edu.upeu.panaderia.exception.MovimientoInvalidoException;
import pe.edu.upeu.panaderia.exception.ProveedorInactivoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasCompraService;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private CompraMapper compraMapper;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private InsumoService insumoService;

    @Mock
    private FinanzasCompraService finanzasService;

    @InjectMocks
    private CompraServiceImpl compraService;

    private Proveedor proveedorActivo() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setRazonSocial("Molino La Estrella S.A.C.");
        proveedor.setActivo(true);
        return proveedor;
    }

    private Proveedor proveedorInactivo() {
        Proveedor proveedor = proveedorActivo();
        proveedor.setActivo(false);
        return proveedor;
    }

    private CompraRequest requestConDosDetalles() {
        return new CompraRequest(
                1L,
                List.of(
                        new DetalleCompraRequest(1L, new BigDecimal("2"), new BigDecimal("3.50")),
                        new DetalleCompraRequest(2L, new BigDecimal("5"), new BigDecimal("2.00"))));
    }

    private CompraResponse compraResponse(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                new ProveedorResumen(1L, "Molino La Estrella S.A.C."),
                compra.getFecha(),
                compra.getEstado(),
                compra.getTotal(),
                List.of());
    }

    @Test
    @DisplayName("Registrar compra correcta calcula subtotales y total, guarda una vez y aumenta cada stock")
    void registrarDeberiaCalcularTotalesAumentarStockYGuardar() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorActivo()));
        when(insumoService.obtener(1L)).thenReturn(insumoDe("Harina de trigo"));
        when(insumoService.obtener(2L)).thenReturn(insumoDe("Azúcar"));
        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocation -> {
                    Compra compra = invocation.getArgument(0);
                    compra.setId(1L);
                    return compra;
                });
        when(compraMapper.toResponse(any(Compra.class)))
                .thenAnswer(invocation -> compraResponse(invocation.getArgument(0)));

        compraService.registrar(requestConDosDetalles());

        verify(insumoService).aumentarStock(1L, new BigDecimal("2"));
        verify(insumoService).aumentarStock(2L, new BigDecimal("5"));
        verify(finanzasService).registrarEgresoCompra(
                eq(1L), any(LocalDateTime.class), eq(new BigDecimal("17.00")));

        ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(captor.capture());

        Compra compra = captor.getValue();
        assertThat(compra.getProveedor().getId()).isEqualTo(1L);
        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.REGISTRADA);
        assertThat(compra.getFecha()).isNotNull();
        assertThat(compra.getTotal()).isEqualByComparingTo(new BigDecimal("17.00"));
        assertThat(compra.getDetalles()).hasSize(2);
        assertThat(compra.getDetalles().get(0).getNombreInsumo()).isEqualTo("Harina de trigo");
        assertThat(compra.getDetalles().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("7.00"));
        assertThat(compra.getDetalles().get(1).getNombreInsumo()).isEqualTo("Azúcar");
        assertThat(compra.getDetalles().get(1).getSubtotal()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Registrar compra aumenta el stock del insumo con la cantidad solicitada")
    void registrarDeberiaAumentarStockDelInsumo() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorActivo()));
        when(insumoService.obtener(1L)).thenReturn(insumoDe("Harina de trigo"));
        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocation -> {
                    Compra compra = invocation.getArgument(0);
                    compra.setId(1L);
                    return compra;
                });
        when(compraMapper.toResponse(any(Compra.class)))
                .thenAnswer(invocation -> compraResponse(invocation.getArgument(0)));
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new DetalleCompraRequest(1L, new BigDecimal("10.500"), new BigDecimal("1.50"))));

        compraService.registrar(request);

        verify(insumoService).aumentarStock(1L, new BigDecimal("10.500"));
        verify(finanzasService).registrarEgresoCompra(
                eq(1L), any(LocalDateTime.class), eq(new BigDecimal("15.75")));
    }

    @Test
    @DisplayName("Registrar compra con proveedor inexistente lanza ResourceNotFoundException y no guarda")
    void registrarConProveedorInexistenteDeberiaLanzarSinGuardar() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());
        CompraRequest request = new CompraRequest(
                99L,
                List.of(new DetalleCompraRequest(1L, new BigDecimal("2"), new BigDecimal("3.50"))));

        assertThatThrownBy(() -> compraService.registrar(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(compraRepository, never()).save(any(Compra.class));
        verify(insumoService, never()).aumentarStock(any(), any());
    }

    @Test
    @DisplayName("Registrar compra con proveedor inactivo lanza ProveedorInactivoException y no guarda")
    void registrarConProveedorInactivoDeberiaLanzarSinGuardar() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorInactivo()));
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new DetalleCompraRequest(1L, new BigDecimal("2"), new BigDecimal("3.50"))));

        assertThatThrownBy(() -> compraService.registrar(request))
                .isInstanceOf(ProveedorInactivoException.class)
                .hasMessageContaining("inactivo");

        verify(compraRepository, never()).save(any(Compra.class));
        verify(insumoService, never()).aumentarStock(any(), any());
    }

    @Test
    @DisplayName("Registrar compra con insumo inexistente lanza ResourceNotFoundException y no guarda")
    void registrarConInsumoInexistenteDeberiaLanzarSinGuardar() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorActivo()));
        when(insumoService.obtener(99L)).thenThrow(new ResourceNotFoundException("Insumo", 99L));
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new DetalleCompraRequest(99L, new BigDecimal("2"), new BigDecimal("3.50"))));

        assertThatThrownBy(() -> compraService.registrar(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(compraRepository, never()).save(any(Compra.class));
        verify(insumoService, never()).aumentarStock(any(), any());
    }

    @Test
    @DisplayName("Registrar compra sin detalles lanza CompraInvalidaException y no guarda")
    void registrarConDetallesVaciosDeberiaLanzarSinGuardar() {
        CompraRequest request = new CompraRequest(1L, List.of());

        assertThatThrownBy(() -> compraService.registrar(request))
                .isInstanceOf(CompraInvalidaException.class)
                .hasMessageContaining("detalle");

        verify(compraRepository, never()).save(any(Compra.class));
        verify(insumoService, never()).aumentarStock(any(), any());
    }

    @Test
    @DisplayName("Fallo en el segundo detalle propaga la excepción y no guarda la compra")
    void registrarConFalloEnSegundoDetalleDeberiaPropagarSinGuardar() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorActivo()));
        when(insumoService.obtener(1L)).thenReturn(insumoDe("Harina de trigo"));
        when(insumoService.obtener(2L)).thenThrow(new ResourceNotFoundException("Insumo", 2L));

        assertThatThrownBy(() -> compraService.registrar(requestConDosDetalles()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("2");

        // El primer detalle ya aumentó stock, pero el rollback de la
        // transacción (@Transactional) revierte ese aumento y evita que
        // la compra se persista.
        verify(insumoService).aumentarStock(1L, new BigDecimal("2"));
        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    @DisplayName("Fallo al registrar el egreso financiero propaga la excepción y revierte la compra")
    void registrarConFalloDelMovimientoFinancieroDeberiaPropagar() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorActivo()));
        when(insumoService.obtener(1L)).thenReturn(insumoDe("Harina de trigo"));
        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocation -> {
                    Compra compra = invocation.getArgument(0);
                    compra.setId(1L);
                    return compra;
                });
        org.mockito.Mockito.doThrow(new MovimientoDuplicadoException("compra", 1L))
                .when(finanzasService).registrarEgresoCompra(
                        eq(1L), any(LocalDateTime.class), any(BigDecimal.class));
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new DetalleCompraRequest(1L, new BigDecimal("2"), new BigDecimal("3.50"))));

        assertThatThrownBy(() -> compraService.registrar(request))
                .isInstanceOf(MovimientoDuplicadoException.class)
                .hasMessageContaining("compra");

        // La compra y el aumento de stock se ejecutaron en memoria, pero el
        // rollback de la transacción (@Transactional) los revierte al fallar
        // el movimiento financiero.
        verify(compraRepository).save(any(Compra.class));
        verify(insumoService).aumentarStock(1L, new BigDecimal("2"));
    }

    @Test
    @DisplayName("Listar compras devuelve todas las registradas")
    void listarDeberiaDevolverCompras() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setEstado(EstadoCompra.REGISTRADA);
        when(compraRepository.findAll()).thenReturn(List.of(compra));
        when(compraMapper.toResponse(any(Compra.class)))
                .thenAnswer(invocation -> compraResponse(invocation.getArgument(0)));

        List<CompraResponse> responses = compraService.listar();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).estado()).isEqualTo(EstadoCompra.REGISTRADA);
    }

    @Test
    @DisplayName("Obtener compra existente devuelve su respuesta")
    void obtenerExistenteDeberiaDevolverCompra() {
        Compra compra = new Compra();
        compra.setId(1L);
        when(compraRepository.findById(1L)).thenReturn(Optional.of(compra));
        when(compraMapper.toResponse(any(Compra.class))).thenReturn(compraResponse(compra));

        CompraResponse response = compraService.obtener(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Obtener compra inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzarExcepcion() {
        when(compraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Anular una compra REGISTRADA descuenta stock, anula el egreso y no la elimina")
    void anularCompraRegistradaDeberiaDescontarStockAnularEgresoYNoEliminar() {
        Compra compra = compraConDosDetalles(EstadoCompra.REGISTRADA);
        when(compraRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(compra));
        when(compraMapper.toResponse(any(Compra.class)))
                .thenAnswer(invocation -> compraResponse(invocation.getArgument(0)));

        CompraResponse response = compraService.anular(1L);

        assertThat(response.estado()).isEqualTo(EstadoCompra.ANULADA);
        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.ANULADA);
        assertThat(compra.getDetalles()).hasSize(2);
        verify(insumoService).descontarStock(1L, new BigDecimal("2"));
        verify(insumoService).descontarStock(2L, new BigDecimal("5"));
        verify(finanzasService).anularEgresoCompra(1L);
        verify(compraRepository, never()).delete(any(Compra.class));
    }

    @Test
    @DisplayName("Anular compra inexistente lanza ResourceNotFoundException y no descuenta")
    void anularCompraInexistenteDeberiaLanzarSinDescontar() {
        when(compraRepository.buscarConBloqueoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.anular(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(insumoService, never()).descontarStock(any(), any());
        verify(finanzasService, never()).anularEgresoCompra(any());
    }

    @Test
    @DisplayName("Anular una compra ya ANULADA lanza CompraYaAnuladaException y no descuenta")
    void anularCompraAnuladaDeberiaLanzarSinDescontar() {
        Compra compra = compraConDosDetalles(EstadoCompra.ANULADA);
        when(compraRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> compraService.anular(1L))
                .isInstanceOf(CompraYaAnuladaException.class)
                .hasMessageContaining("ya está anulada");

        verify(insumoService, never()).descontarStock(any(), any());
        verify(finanzasService, never()).anularEgresoCompra(any());
    }

    @Test
    @DisplayName("Fallo de stock en un detalle propaga la excepción y no cambia el estado")
    void anularConStockInsuficienteDeberiaPropagarSinCambiarEstado() {
        Compra compra = compraConDosDetalles(EstadoCompra.REGISTRADA);
        when(compraRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(compra));
        org.mockito.Mockito.doNothing().when(insumoService).descontarStock(1L, new BigDecimal("2"));
        org.mockito.Mockito.doThrow(
                        new InsumoStockInsuficienteException(2L, new BigDecimal("5"), new BigDecimal("2")))
                .when(insumoService).descontarStock(eq(2L), eq(new BigDecimal("5")));

        assertThatThrownBy(() -> compraService.anular(1L))
                .isInstanceOf(InsumoStockInsuficienteException.class)
                .hasMessageContaining("solicitó");

        verify(insumoService).descontarStock(1L, new BigDecimal("2"));
        verify(finanzasService, never()).anularEgresoCompra(any());
        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.REGISTRADA);
    }

    @Test
    @DisplayName("Fallo al anular el egreso financiero propaga y la transacción revierte la anulación")
    void anularConFalloFinancieroDeberiaPropagarYConservarDetalles() {
        Compra compra = compraConDosDetalles(EstadoCompra.REGISTRADA);
        when(compraRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(compra));
        org.mockito.Mockito.doThrow(
                        new MovimientoInvalidoException("El egreso de la compra 1 ya está anulado"))
                .when(finanzasService).anularEgresoCompra(1L);

        assertThatThrownBy(() -> compraService.anular(1L))
                .isInstanceOf(MovimientoInvalidoException.class)
                .hasMessageContaining("ya está anulado");

        // Los descuentos de stock y el cambio de estado se ejecutaron en
        // memoria, pero el rollback de la transacción (@Transactional) los
        // revierte al fallar la anulación del egreso.
        verify(insumoService).descontarStock(1L, new BigDecimal("2"));
        verify(insumoService).descontarStock(2L, new BigDecimal("5"));
        assertThat(compra.getDetalles()).hasSize(2);
        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.ANULADA);
    }

    private Compra compraConDosDetalles(EstadoCompra estado) {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setProveedor(proveedorActivo());
        compra.setFecha(LocalDateTime.of(2026, 9, 14, 11, 0));
        compra.setEstado(estado);
        compra.setTotal(new BigDecimal("17.00"));

        DetalleCompra primero = new DetalleCompra();
        primero.setInsumoId(1L);
        primero.setNombreInsumo("Harina de trigo");
        primero.setCantidad(new BigDecimal("2"));
        compra.agregarDetalle(primero);

        DetalleCompra segundo = new DetalleCompra();
        segundo.setInsumoId(2L);
        segundo.setNombreInsumo("Azúcar");
        segundo.setCantidad(new BigDecimal("5"));
        compra.agregarDetalle(segundo);
        return compra;
    }

    private InsumoResponse insumoDe(String nombre) {
        return InsumoResponse.builder()
                .id(1L)
                .nombre(nombre)
                .unidadMedida("KILOGRAMO")
                .stockActual(new BigDecimal("50.000"))
                .stockMinimo(new BigDecimal("10.000"))
                .build();
    }
}
