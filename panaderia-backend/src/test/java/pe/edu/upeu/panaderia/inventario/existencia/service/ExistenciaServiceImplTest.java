package pe.edu.upeu.panaderia.inventario.existencia.service;

import java.math.BigDecimal;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.exception.ExistenciaConStockException;
import pe.edu.upeu.panaderia.exception.ExistenciaDuplicadaException;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.StockInsuficienteException;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaRequest;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaResponse;
import pe.edu.upeu.panaderia.inventario.existencia.entity.Existencia;
import pe.edu.upeu.panaderia.inventario.existencia.mapper.ExistenciaMapper;
import pe.edu.upeu.panaderia.inventario.existencia.repository.ExistenciaRepository;

@ExtendWith(MockitoExtension.class)
class ExistenciaServiceImplTest {

    @Mock
    private ExistenciaRepository existenciaRepository;

    @Mock
    private ExistenciaMapper existenciaMapper;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ExistenciaServiceImpl existenciaService;

    private Existencia existenciaConCantidad(Long productoId, String cantidad) {
        Existencia existencia = new Existencia();
        existencia.setId(1L);
        existencia.setIdProducto(productoId);
        existencia.setCantidad(new BigDecimal(cantidad));
        return existencia;
    }

    private ProductoResponse producto(Long id, String nombre) {
        return ProductoResponse.builder().id(id).nombre(nombre).build();
    }

    private ExistenciaResponse response(Long id, Long productoId, String nombre, String cantidad) {
        return new ExistenciaResponse(id,
                new ExistenciaResponse.ProductoResumen(productoId, nombre),
                new BigDecimal(cantidad));
    }

    private ExistenciaRequest request(Long productoId, String cantidad) {
        ExistenciaRequest existenciaRequest = new ExistenciaRequest();
        existenciaRequest.setProductoId(productoId);
        existenciaRequest.setCantidad(new BigDecimal(cantidad));
        return existenciaRequest;
    }

    @Test
    @DisplayName("Descontar stock exitoso: 100 - 2 = 98")
    void descontarDeberiaRestarCantidad() {
        Existencia existencia = existenciaConCantidad(1L, "100.000");
        when(existenciaRepository.buscarConBloqueoPorProducto(1L)).thenReturn(Optional.of(existencia));

        existenciaService.descontar(1L, new BigDecimal("2.000"));

        assertThat(existencia.getCantidad()).isEqualByComparingTo(new BigDecimal("98.000"));
    }

    @Test
    @DisplayName("Descontar con stock insuficiente lanza StockInsuficienteException y no cambia la cantidad")
    void descontarConStockInsuficienteDeberiaLanzarExcepcion() {
        Existencia existencia = existenciaConCantidad(1L, "10.000");
        when(existenciaRepository.buscarConBloqueoPorProducto(1L)).thenReturn(Optional.of(existencia));

        assertThatThrownBy(() -> existenciaService.descontar(1L, new BigDecimal("15.000")))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(existencia.getCantidad()).isEqualByComparingTo(new BigDecimal("10.000"));
    }

    @Test
    @DisplayName("Descontar sin existencia lanza ResourceNotFoundException")
    void descontarSinExistenciaDeberiaLanzarExcepcion() {
        when(existenciaRepository.buscarConBloqueoPorProducto(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> existenciaService.descontar(99L, new BigDecimal("2.000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Reponer stock exitoso: 95 + 3 = 98")
    void reponerDeberiaSumarCantidad() {
        Existencia existencia = existenciaConCantidad(1L, "95.000");
        when(existenciaRepository.buscarConBloqueoPorProducto(1L)).thenReturn(Optional.of(existencia));

        existenciaService.reponer(1L, new BigDecimal("3.000"));

        assertThat(existencia.getCantidad()).isEqualByComparingTo(new BigDecimal("98.000"));
    }

    @Test
    @DisplayName("Reponer con cantidad inválida lanza excepción y no modifica la cantidad")
    void reponerConCantidadInvalidaDeberiaLanzarSinModificar() {
        Existencia existencia = existenciaConCantidad(1L, "95.000");
        when(existenciaRepository.buscarConBloqueoPorProducto(1L)).thenReturn(Optional.of(existencia));

        assertThatThrownBy(() -> existenciaService.reponer(1L, new BigDecimal("0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor que cero");

        assertThat(existencia.getCantidad()).isEqualByComparingTo(new BigDecimal("95.000"));
    }

    @Test
    @DisplayName("Reponer sin existencia lanza ResourceNotFoundException")
    void reponerSinExistenciaDeberiaLanzarExcepcion() {
        when(existenciaRepository.buscarConBloqueoPorProducto(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> existenciaService.reponer(99L, new BigDecimal("3.000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Listar sin filtro devuelve todas las existencias enriquecidas con el nombre del producto")
    void listarSinFiltroDeberiaDevolverTodas() {
        Existencia existencia = existenciaConCantidad(7L, "12.500");
        when(existenciaRepository.findAll()).thenReturn(List.of(existencia));
        when(productoService.obtener(7L)).thenReturn(producto(7L, "Pan"));
        when(existenciaMapper.toResponse(eq(existencia), any(ProductoResponse.class)))
                .thenReturn(response(1L, 7L, "Pan", "12.500"));

        List<ExistenciaResponse> resultado = existenciaService.listar(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).producto().nombre()).isEqualTo("Pan");
        verify(existenciaRepository, never()).findAllByIdProductoOrderByIdAsc(any());
    }

    @Test
    @DisplayName("Listar con productoId filtra en el backend, no descarga toda la lista")
    void listarConFiltroDeberiaConsultarSoloEseProducto() {
        Existencia existencia = existenciaConCantidad(7L, "12.500");
        when(existenciaRepository.findAllByIdProductoOrderByIdAsc(7L)).thenReturn(List.of(existencia));
        when(productoService.obtener(7L)).thenReturn(producto(7L, "Pan"));
        when(existenciaMapper.toResponse(eq(existencia), any(ProductoResponse.class)))
                .thenReturn(response(1L, 7L, "Pan", "12.500"));

        List<ExistenciaResponse> resultado = existenciaService.listar(7L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).producto().id()).isEqualTo(7L);
        verify(existenciaRepository, never()).findAll();
    }

    @Test
    @DisplayName("Listar con productoId no positivo lanza FiltroInvalidoException sin consultar el repositorio")
    void listarConFiltroNoPositivoDeberiaLanzarExcepcion() {
        assertThatThrownBy(() -> existenciaService.listar(0L))
                .isInstanceOf(FiltroInvalidoException.class)
                .hasMessageContaining("productoId");

        verify(existenciaRepository, never()).findAll();
        verify(existenciaRepository, never()).findAllByIdProductoOrderByIdAsc(any());
    }

    @Test
    @DisplayName("Obtener por id devuelve la existencia con el resumen del producto")
    void obtenerDeberiaEnriquecerConElProducto() {
        Existencia existencia = existenciaConCantidad(3L, "5.000");
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existencia));
        when(productoService.obtener(3L)).thenReturn(producto(3L, "Croissant"));
        when(existenciaMapper.toResponse(eq(existencia), any(ProductoResponse.class)))
                .thenReturn(response(1L, 3L, "Croissant", "5.000"));

        ExistenciaResponse resultado = existenciaService.obtener(1L);

        assertThat(resultado.producto().nombre()).isEqualTo("Croissant");
        assertThat(resultado.cantidad()).isEqualByComparingTo("5.000");
    }

    @Test
    @DisplayName("Obtener por id inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzarExcepcion() {
        when(existenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> existenciaService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Obtener por producto devuelve la existencia asociada")
    void obtenerPorProductoDeberiaEnriquecerConElProducto() {
        Existencia existencia = existenciaConCantidad(4L, "8.000");
        when(existenciaRepository.findByIdProducto(4L)).thenReturn(Optional.of(existencia));
        when(productoService.obtener(4L)).thenReturn(producto(4L, "Torta"));
        when(existenciaMapper.toResponse(eq(existencia), any(ProductoResponse.class)))
                .thenReturn(response(1L, 4L, "Torta", "8.000"));

        assertThat(existenciaService.obtenerPorProducto(4L).producto().nombre()).isEqualTo("Torta");
    }

    @Test
    @DisplayName("Crear registra la existencia con cantidad cero o mayor")
    void crearDeberiaRegistrarExistencia() {
        when(productoService.obtener(2L)).thenReturn(producto(2L, "Pan"));
        when(existenciaRepository.existsByIdProducto(2L)).thenReturn(false);
        when(existenciaRepository.save(any(Existencia.class))).thenAnswer(invocacion -> {
            Existencia guardada = invocacion.getArgument(0);
            guardada.setId(20L);
            return guardada;
        });
        when(existenciaMapper.toResponse(any(Existencia.class), any(ProductoResponse.class)))
                .thenReturn(response(20L, 2L, "Pan", "0.000"));

        ExistenciaResponse resultado = existenciaService.crear(request(2L, "0.000"));

        assertThat(resultado.id()).isEqualTo(20L);
        assertThat(resultado.producto().nombre()).isEqualTo("Pan");
        assertThat(resultado.cantidad()).isEqualByComparingTo("0.000");
    }

    @Test
    @DisplayName("Crear con producto inexistente propaga ResourceNotFoundException y no guarda")
    void crearConProductoInexistenteDeberiaLanzarExcepcion() {
        when(productoService.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Producto", 99L));

        assertThatThrownBy(() -> existenciaService.crear(request(99L, "5.000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(existenciaRepository, never()).save(any(Existencia.class));
    }

    @Test
    @DisplayName("Crear cuando el producto ya tiene existencia lanza ExistenciaDuplicadaException")
    void crearConProductoDuplicadoDeberiaLanzarExcepcion() {
        when(productoService.obtener(2L)).thenReturn(producto(2L, "Pan"));
        when(existenciaRepository.existsByIdProducto(2L)).thenReturn(true);

        assertThatThrownBy(() -> existenciaService.crear(request(2L, "5.000")))
                .isInstanceOf(ExistenciaDuplicadaException.class)
                .hasMessageContaining("ya tiene una existencia");

        verify(existenciaRepository, never()).save(any(Existencia.class));
    }

    @Test
    @DisplayName("Actualizar cambia la cantidad manteniendo el mismo producto")
    void actualizarDeberiaCambiarCantidad() {
        Existencia existencia = existenciaConCantidad(5L, "10.000");
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existencia));
        when(productoService.obtener(5L)).thenReturn(producto(5L, "Pan"));
        when(existenciaMapper.toResponse(eq(existencia), any(ProductoResponse.class)))
                .thenReturn(response(1L, 5L, "Pan", "25.500"));

        ExistenciaResponse resultado = existenciaService.actualizar(1L, request(5L, "25.500"));

        assertThat(resultado.cantidad()).isEqualByComparingTo("25.500");
        assertThat(existencia.getIdProducto()).isEqualTo(5L);
        assertThat(existencia.getCantidad()).isEqualByComparingTo("25.500");
        verify(existenciaRepository, never()).existsByIdProducto(any());
    }

    @Test
    @DisplayName("Actualizar permite cambiar el producto si el nuevo aún no tiene existencia")
    void actualizarDeberiaPermitirCambiarProducto() {
        Existencia existencia = existenciaConCantidad(5L, "10.000");
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existencia));
        when(productoService.obtener(6L)).thenReturn(producto(6L, "Bizcocho"));
        when(existenciaRepository.existsByIdProducto(6L)).thenReturn(false);
        when(existenciaMapper.toResponse(eq(existencia), any(ProductoResponse.class)))
                .thenReturn(response(1L, 6L, "Bizcocho", "10.000"));

        ExistenciaResponse resultado = existenciaService.actualizar(1L, request(6L, "10.000"));

        assertThat(resultado.producto().id()).isEqualTo(6L);
        assertThat(existencia.getIdProducto()).isEqualTo(6L);
    }

    @Test
    @DisplayName("Actualizar hacia un producto que ya tiene otra existencia lanza ExistenciaDuplicadaException")
    void actualizarConProductoDuplicadoDeberiaLanzarExcepcion() {
        Existencia existencia = existenciaConCantidad(5L, "10.000");
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existencia));
        when(productoService.obtener(6L)).thenReturn(producto(6L, "Bizcocho"));
        when(existenciaRepository.existsByIdProducto(6L)).thenReturn(true);

        assertThatThrownBy(() -> existenciaService.actualizar(1L, request(6L, "10.000")))
                .isInstanceOf(ExistenciaDuplicadaException.class)
                .hasMessageContaining("ya tiene una existencia");

        assertThat(existencia.getIdProducto()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Actualizar con producto inexistente propaga ResourceNotFoundException")
    void actualizarConProductoInexistenteDeberiaLanzarExcepcion() {
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existenciaConCantidad(5L, "10.000")));
        when(productoService.obtener(99L))
                .thenThrow(new ResourceNotFoundException("Producto", 99L));

        assertThatThrownBy(() -> existenciaService.actualizar(1L, request(99L, "10.000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Actualizar una existencia inexistente lanza ResourceNotFoundException")
    void actualizarInexistenteDeberiaLanzarExcepcion() {
        when(existenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> existenciaService.actualizar(99L, request(5L, "10.000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar una existencia con cantidad exactamente cero la borra")
    void eliminarConCantidadCeroDeberiaBorrar() {
        Existencia existencia = existenciaConCantidad(5L, "0.000");
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existencia));

        existenciaService.eliminar(1L);

        verify(existenciaRepository).delete(existencia);
    }

    @Test
    @DisplayName("Eliminar con cantidad positiva lanza ExistenciaConStockException y no borra")
    void eliminarConCantidadPositivaDeberiaLanzarExcepcion() {
        Existencia existencia = existenciaConCantidad(5L, "3.500");
        when(existenciaRepository.findById(1L)).thenReturn(Optional.of(existencia));

        assertThatThrownBy(() -> existenciaService.eliminar(1L))
                .isInstanceOf(ExistenciaConStockException.class)
                .hasMessageContaining("Solo se permite eliminar existencias con cantidad cero");

        verify(existenciaRepository, never()).delete(any(Existencia.class));
    }

    @Test
    @DisplayName("Eliminar una existencia inexistente lanza ResourceNotFoundException")
    void eliminarInexistenteDeberiaLanzarExcepcion() {
        when(existenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> existenciaService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(existenciaRepository, never()).delete(any(Existencia.class));
    }
}
