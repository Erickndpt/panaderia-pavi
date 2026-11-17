package pe.edu.upeu.panaderia.produccion.orden.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.exception.InsumoStockInsuficienteException;
import pe.edu.upeu.panaderia.exception.OrdenProduccionInvalidaException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;
import pe.edu.upeu.panaderia.produccion.orden.dto.DetalleConsumoRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionResponse;
import pe.edu.upeu.panaderia.produccion.orden.entity.EstadoOrdenProduccion;
import pe.edu.upeu.panaderia.produccion.orden.entity.OrdenProduccion;
import pe.edu.upeu.panaderia.produccion.orden.mapper.OrdenProduccionMapper;
import pe.edu.upeu.panaderia.produccion.orden.repository.OrdenProduccionRepository;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionServiceImplTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;

    @Mock
    private OrdenProduccionMapper ordenProduccionMapper;

    @Mock
    private ProductoService productoService;

    @Mock
    private InsumoService insumoService;

    @Mock
    private ExistenciaService existenciaService;

    @InjectMocks
    private OrdenProduccionServiceImpl ordenProduccionService;

    private ProductoResponse productoPanFrances() {
        return ProductoResponse.builder()
                .id(1L)
                .nombre("Pan francés")
                .precio(new BigDecimal("1.50"))
                .build();
    }

    private InsumoResponse insumoDe(Long id, String nombre) {
        return InsumoResponse.builder()
                .id(id)
                .nombre(nombre)
                .unidadMedida("KILOGRAMO")
                .stockActual(new BigDecimal("50.000"))
                .stockMinimo(new BigDecimal("10.000"))
                .build();
    }

    private OrdenProduccionRequest requestConDosInsumos() {
        return new OrdenProduccionRequest(
                1L,
                new BigDecimal("12"),
                List.of(
                        new DetalleConsumoRequest(1L, new BigDecimal("2.500")),
                        new DetalleConsumoRequest(2L, new BigDecimal("0.700"))));
    }

    private OrdenProduccionResponse ordenResponse(OrdenProduccion orden) {
        return new OrdenProduccionResponse(
                orden.getId(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getProductoId(),
                orden.getNombreProducto(),
                orden.getCantidadProducida(),
                List.of());
    }

    @Test
    @DisplayName("Registrar orden correcta descuenta cada insumo, repone el producto y guarda con fotografías")
    void registrarDeberiaDescontarReponerYGuardarConFotografias() {
        when(productoService.obtener(1L)).thenReturn(productoPanFrances());
        when(insumoService.obtener(1L)).thenReturn(insumoDe(1L, "Harina de trigo"));
        when(insumoService.obtener(2L)).thenReturn(insumoDe(2L, "Azúcar"));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ordenProduccionMapper.toResponse(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> ordenResponse(invocation.getArgument(0)));

        ordenProduccionService.registrar(requestConDosInsumos());

        verify(insumoService).descontarStock(1L, new BigDecimal("2.500"));
        verify(insumoService).descontarStock(2L, new BigDecimal("0.700"));
        verify(existenciaService).reponer(1L, new BigDecimal("12"));

        ArgumentCaptor<OrdenProduccion> captor = ArgumentCaptor.forClass(OrdenProduccion.class);
        verify(ordenProduccionRepository).save(captor.capture());

        OrdenProduccion orden = captor.getValue();
        assertThat(orden.getProductoId()).isEqualTo(1L);
        assertThat(orden.getNombreProducto()).isEqualTo("Pan francés");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenProduccion.COMPLETADA);
        assertThat(orden.getFecha()).isNotNull();
        assertThat(orden.getCantidadProducida()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(orden.getDetalles()).hasSize(2);
        assertThat(orden.getDetalles().get(0).getInsumoId()).isEqualTo(1L);
        assertThat(orden.getDetalles().get(0).getNombreInsumo()).isEqualTo("Harina de trigo");
        assertThat(orden.getDetalles().get(0).getUnidadMedida()).isEqualTo("KILOGRAMO");
        assertThat(orden.getDetalles().get(0).getCantidadConsumida()).isEqualByComparingTo(new BigDecimal("2.500"));
        assertThat(orden.getDetalles().get(1).getNombreInsumo()).isEqualTo("Azúcar");
    }

    @Test
    @DisplayName("Registrar orden descuenta cada insumo con la cantidad consumida solicitada")
    void registrarDeberiaDescontarCadaInsumo() {
        when(productoService.obtener(1L)).thenReturn(productoPanFrances());
        when(insumoService.obtener(1L)).thenReturn(insumoDe(1L, "Harina de trigo"));
        when(insumoService.obtener(2L)).thenReturn(insumoDe(2L, "Azúcar"));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ordenProduccionMapper.toResponse(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> ordenResponse(invocation.getArgument(0)));

        ordenProduccionService.registrar(requestConDosInsumos());

        verify(insumoService).descontarStock(1L, new BigDecimal("2.500"));
        verify(insumoService).descontarStock(2L, new BigDecimal("0.700"));
    }

    @Test
    @DisplayName("Registrar orden repone la existencia del producto terminado")
    void registrarDeberiaReponerLaExistenciaDelProducto() {
        when(productoService.obtener(1L)).thenReturn(productoPanFrances());
        when(insumoService.obtener(1L)).thenReturn(insumoDe(1L, "Harina de trigo"));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ordenProduccionMapper.toResponse(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> ordenResponse(invocation.getArgument(0)));
        OrdenProduccionRequest request = new OrdenProduccionRequest(
                1L,
                new BigDecimal("30"),
                List.of(new DetalleConsumoRequest(1L, new BigDecimal("2.500"))));

        ordenProduccionService.registrar(request);

        verify(existenciaService).reponer(1L, new BigDecimal("30"));
    }

    @Test
    @DisplayName("Registrar orden con producto inexistente lanza y no descuenta ni guarda ni repone")
    void registrarConProductoInexistenteDeberiaLanzarSinEfectos() {
        when(productoService.obtener(99L)).thenThrow(new ResourceNotFoundException("Producto", 99L));
        OrdenProduccionRequest request = new OrdenProduccionRequest(
                99L,
                new BigDecimal("12"),
                List.of(new DetalleConsumoRequest(1L, new BigDecimal("2.500"))));

        assertThatThrownBy(() -> ordenProduccionService.registrar(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(insumoService, never()).descontarStock(any(), any());
        verify(existenciaService, never()).reponer(any(), any());
        verify(ordenProduccionRepository, never()).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Registrar orden con insumo inexistente lanza y no guarda ni repone")
    void registrarConInsumoInexistenteDeberiaLanzarSinGuardar() {
        when(productoService.obtener(1L)).thenReturn(productoPanFrances());
        when(insumoService.obtener(1L)).thenReturn(insumoDe(1L, "Harina de trigo"));
        when(insumoService.obtener(2L)).thenThrow(new ResourceNotFoundException("Insumo", 2L));

        assertThatThrownBy(() -> ordenProduccionService.registrar(requestConDosInsumos()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("2");

        verify(existenciaService, never()).reponer(any(), any());
        verify(ordenProduccionRepository, never()).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Registrar orden con insumo duplicado lanza 400 y no consulta producto ni descuenta ni guarda")
    void registrarConInsumoDuplicadoDeberiaLanzarSinEfectos() {
        OrdenProduccionRequest request = new OrdenProduccionRequest(
                1L,
                new BigDecimal("12"),
                List.of(
                        new DetalleConsumoRequest(1L, new BigDecimal("2.500")),
                        new DetalleConsumoRequest(1L, new BigDecimal("1.000"))));

        assertThatThrownBy(() -> ordenProduccionService.registrar(request))
                .isInstanceOf(OrdenProduccionInvalidaException.class)
                .hasMessageContaining("más de una vez");

        verify(productoService, never()).obtener(any());
        verify(insumoService, never()).descontarStock(any(), any());
        verify(existenciaService, never()).reponer(any(), any());
        verify(ordenProduccionRepository, never()).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Registrar orden sin insumos lanza 400 y no produce efectos")
    void registrarConInsumosVaciosDeberiaLanzarSinEfectos() {
        OrdenProduccionRequest request = new OrdenProduccionRequest(1L, new BigDecimal("12"), List.of());

        assertThatThrownBy(() -> ordenProduccionService.registrar(request))
                .isInstanceOf(OrdenProduccionInvalidaException.class)
                .hasMessageContaining("insumo");

        verify(productoService, never()).obtener(any());
        verify(insumoService, never()).descontarStock(any(), any());
        verify(existenciaService, never()).reponer(any(), any());
        verify(ordenProduccionRepository, never()).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Registrar orden con stock insuficiente de un insumo propaga el conflicto y no guarda ni repone")
    void registrarConStockInsuficienteDeberiaPropagarSinGuardar() {
        when(productoService.obtener(1L)).thenReturn(productoPanFrances());
        when(insumoService.obtener(1L)).thenReturn(insumoDe(1L, "Harina de trigo"));
        doThrow(new InsumoStockInsuficienteException(
                1L, new BigDecimal("2.500"), new BigDecimal("1.000")))
                .when(insumoService).descontarStock(1L, new BigDecimal("2.500"));

        assertThatThrownBy(() -> ordenProduccionService.registrar(requestConDosInsumos()))
                .isInstanceOf(InsumoStockInsuficienteException.class)
                .hasMessageContaining("2.500")
                .hasMessageContaining("1.000");

        verify(existenciaService, never()).reponer(any(), any());
        verify(ordenProduccionRepository, never()).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Fallo en el segundo detalle propaga la excepción, sin guardar ni reponer (rollback revierte el primer descuento)")
    void registrarConFalloEnSegundoDetalleDeberiaPropagarSinGuardarNiReponer() {
        when(productoService.obtener(1L)).thenReturn(productoPanFrances());
        when(insumoService.obtener(1L)).thenReturn(insumoDe(1L, "Harina de trigo"));
        when(insumoService.obtener(2L)).thenReturn(insumoDe(2L, "Azúcar"));
        doNothing().when(insumoService).descontarStock(1L, new BigDecimal("2.500"));
        doThrow(new InsumoStockInsuficienteException(
                2L, new BigDecimal("0.700"), new BigDecimal("0.300")))
                .when(insumoService).descontarStock(2L, new BigDecimal("0.700"));

        assertThatThrownBy(() -> ordenProduccionService.registrar(requestConDosInsumos()))
                .isInstanceOf(InsumoStockInsuficienteException.class)
                .hasMessageContaining("2");

        verify(insumoService).descontarStock(1L, new BigDecimal("2.500"));
        verify(existenciaService, never()).reponer(any(), any());
        verify(ordenProduccionRepository, never()).save(any(OrdenProduccion.class));
    }

    @Test
    @DisplayName("Listar órdenes de producción devuelve todas las registradas")
    void listarDeberiaDevolverOrdenes() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setEstado(EstadoOrdenProduccion.COMPLETADA);
        when(ordenProduccionRepository.findAll()).thenReturn(List.of(orden));
        when(ordenProduccionMapper.toResponse(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> ordenResponse(invocation.getArgument(0)));

        List<OrdenProduccionResponse> responses = ordenProduccionService.listar();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).estado()).isEqualTo(EstadoOrdenProduccion.COMPLETADA);
    }

    @Test
    @DisplayName("Obtener orden existente devuelve su respuesta")
    void obtenerExistenteDeberiaDevolverOrden() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenProduccionMapper.toResponse(any(OrdenProduccion.class)))
                .thenAnswer(invocation -> ordenResponse(invocation.getArgument(0)));

        OrdenProduccionResponse response = ordenProduccionService.obtener(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Obtener orden inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzarExcepcion() {
        when(ordenProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenProduccionService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}