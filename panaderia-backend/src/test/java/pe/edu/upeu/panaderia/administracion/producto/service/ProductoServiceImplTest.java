package pe.edu.upeu.panaderia.administracion.producto.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upeu.panaderia.administracion.categoria.entity.Categoria;
import pe.edu.upeu.panaderia.administracion.categoria.repository.CategoriaRepository;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.entity.Producto;
import pe.edu.upeu.panaderia.administracion.producto.mapper.ProductoMapper;
import pe.edu.upeu.panaderia.administracion.producto.repository.ProductoRepository;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @Test
    @DisplayName("Rango de precios inválido lanza FiltroInvalidoException sin consultar datos")
    void listarConRangoDePreciosInvalidoDeberiaLanzarExcepcion() {
        assertThatThrownBy(() -> productoService.listar(null, null, new BigDecimal("10"), new BigDecimal("2")))
                .isInstanceOf(FiltroInvalidoException.class)
                .hasMessageContaining("precioMin");
    }

    @Test
    @DisplayName("Sin filtros lista todos los productos")
    void listarSinFiltrosDeberiaDelegarAlListadoCompleto() {
        when(productoRepository.findAll()).thenReturn(List.of());

        List<ProductoResponse> resultado = productoService.listar(null, null, null, null);

        assertThat(resultado).isEmpty();
        verify(productoRepository).findAll();
    }

    @Test
    @DisplayName("Con filtros combina Specification, valida categoría y mapea los resultados")
    void listarConFiltrosDeberiaUsarSpecification() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Panificación");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Pan Integral");
        producto.setPrecio(new BigDecimal("3.50"));
        when(productoRepository.findAll(any(Specification.class))).thenReturn(List.of(producto));
        ProductoResponse esperado = ProductoResponse.builder()
                .id(1L)
                .nombre("Pan Integral")
                .precio(new BigDecimal("3.50"))
                .build();
        when(productoMapper.toResponse(producto)).thenReturn(esperado);

        List<ProductoResponse> resultado = productoService.listar(
                "integral", 1L, new BigDecimal("2"), new BigDecimal("10"));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Pan Integral");
        verify(productoRepository).findAll(any(Specification.class));
    }
}