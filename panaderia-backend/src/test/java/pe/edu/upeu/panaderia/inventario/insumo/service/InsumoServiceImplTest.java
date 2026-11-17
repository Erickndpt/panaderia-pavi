package pe.edu.upeu.panaderia.inventario.insumo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.panaderia.exception.InsumoDuplicadoException;
import pe.edu.upeu.panaderia.exception.InsumoStockInsuficienteException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoRequest;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.entity.Insumo;
import pe.edu.upeu.panaderia.inventario.insumo.mapper.InsumoMapper;
import pe.edu.upeu.panaderia.inventario.insumo.repository.InsumoRepository;

@ExtendWith(MockitoExtension.class)
class InsumoServiceImplTest {

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private InsumoMapper insumoMapper;

    @InjectMocks
    private InsumoServiceImpl insumoService;

    private InsumoRequest requestValido() {
        InsumoRequest request = new InsumoRequest();
        request.setNombre("Harina de trigo");
        request.setUnidadMedida("KILOGRAMO");
        request.setStockActual(new BigDecimal("50.000"));
        request.setStockMinimo(new BigDecimal("10.000"));
        return request;
    }

    private Insumo insumoValido() {
        Insumo insumo = new Insumo();
        insumo.setId(1L);
        insumo.setNombre("Harina de trigo");
        insumo.setUnidadMedida("KILOGRAMO");
        insumo.setStockActual(new BigDecimal("50.000"));
        insumo.setStockMinimo(new BigDecimal("10.000"));
        return insumo;
    }

    private InsumoResponse responseValido() {
        return InsumoResponse.builder()
                .id(1L)
                .nombre("Harina de trigo")
                .unidadMedida("KILOGRAMO")
                .stockActual(new BigDecimal("50.000"))
                .stockMinimo(new BigDecimal("10.000"))
                .build();
    }

    @Test
    @DisplayName("Listar insumos devuelve todos los registrados")
    void listarDeberiaDevolverInsumos() {
        when(insumoRepository.findAll()).thenReturn(List.of(insumoValido()));
        when(insumoMapper.toResponse(any(Insumo.class))).thenReturn(responseValido());

        List<InsumoResponse> responses = insumoService.listar();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getNombre()).isEqualTo("Harina de trigo");
    }

    @Test
    @DisplayName("Obtener insumo existente devuelve su respuesta")
    void obtenerExistenteDeberiaDevolverInsumo() {
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumoValido()));
        when(insumoMapper.toResponse(any(Insumo.class))).thenReturn(responseValido());

        InsumoResponse response = insumoService.obtener(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUnidadMedida()).isEqualTo("KILOGRAMO");
    }

    @Test
    @DisplayName("Obtener insumo inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzarExcepcion() {
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insumoService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Crear insumo válido guarda y devuelve la respuesta")
    void crearDeberiaGuardarYDevolverInsumo() {
        when(insumoRepository.findByNombreIgnoreCase("Harina de trigo")).thenReturn(Optional.empty());
        when(insumoMapper.toEntity(any(InsumoRequest.class))).thenReturn(insumoValido());
        when(insumoRepository.save(any(Insumo.class))).thenReturn(insumoValido());
        when(insumoMapper.toResponse(any(Insumo.class))).thenReturn(responseValido());

        InsumoResponse response = insumoService.crear(requestValido());

        assertThat(response.getNombre()).isEqualTo("Harina de trigo");
        verify(insumoRepository).save(any(Insumo.class));
    }

    @Test
    @DisplayName("Crear insumo con nombre duplicado lanza InsumoDuplicadoException y no guarda")
    void crearConNombreDuplicadoDeberiaLanzarSinGuardar() {
        when(insumoRepository.findByNombreIgnoreCase("Harina de trigo")).thenReturn(Optional.of(insumoValido()));

        assertThatThrownBy(() -> insumoService.crear(requestValido()))
                .isInstanceOf(InsumoDuplicadoException.class)
                .hasMessageContaining("Harina de trigo");

        verify(insumoRepository, never()).save(any(Insumo.class));
    }

    @Test
    @DisplayName("Actualizar insumo existente conservando su nombre modifica sin conflicto")
    void actualizarDeberiaModificarElInsumoExistente() {
        Insumo insumo = insumoValido();
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.findByNombreIgnoreCase("Harina de trigo")).thenReturn(Optional.of(insumo));
        when(insumoMapper.toResponse(any(Insumo.class))).thenReturn(responseValido());

        InsumoResponse response = insumoService.actualizar(1L, requestValido());

        assertThat(response.getStockActual()).isEqualByComparingTo(new BigDecimal("50.000"));
    }

    @Test
    @DisplayName("Actualizar insumo con nombre usado por otro insumo lanza InsumoDuplicadoException")
    void actualizarConNombreDeOtroInsumoDeberiaLanzarExcepcion() {
        Insumo insumo = insumoValido();
        Insumo otroInsumo = insumoValido();
        otroInsumo.setId(2L);
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.findByNombreIgnoreCase("Harina de trigo")).thenReturn(Optional.of(otroInsumo));

        assertThatThrownBy(() -> insumoService.actualizar(1L, requestValido()))
                .isInstanceOf(InsumoDuplicadoException.class)
                .hasMessageContaining("Harina de trigo");
    }

    @Test
    @DisplayName("Actualizar insumo inexistente lanza ResourceNotFoundException")
    void actualizarInexistenteDeberiaLanzarExcepcion() {
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insumoService.actualizar(99L, requestValido()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar insumo existente borra el registro")
    void eliminarDeberiaBorrarElInsumoExistente() {
        Insumo insumo = insumoValido();
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumo));

        insumoService.eliminar(1L);

        verify(insumoRepository).delete(insumo);
    }

    @Test
    @DisplayName("Eliminar insumo inexistente lanza ResourceNotFoundException y no borra")
    void eliminarInexistenteDeberiaLanzarExcepcion() {
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insumoService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(insumoRepository, never()).delete(any(Insumo.class));
    }

    @Test
    @DisplayName("Descontar stock decrementa la cantidad actual del insumo")
    void descontarStockDeberiaDecrementarElStockActual() {
        Insumo insumo = insumoValido();
        when(insumoRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(insumo));

        insumoService.descontarStock(1L, new BigDecimal("3.500"));

        assertThat(insumo.getStockActual()).isEqualByComparingTo(new BigDecimal("46.500"));
    }

    @Test
    @DisplayName("Descontar stock con stock insuficiente lanza con lo solicitado y disponible y no modifica")
    void descontarStockConStockInsuficienteDeberiaLanzarSinCambiar() {
        Insumo insumo = insumoValido();
        when(insumoRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(insumo));

        assertThatThrownBy(() -> insumoService.descontarStock(1L, new BigDecimal("60")))
                .isInstanceOf(InsumoStockInsuficienteException.class)
                .hasMessageContaining("60")
                .hasMessageContaining("50");

        assertThat(insumo.getStockActual()).isEqualByComparingTo(new BigDecimal("50.000"));
    }

    @Test
    @DisplayName("Descontar stock de insumo inexistente lanza ResourceNotFoundException")
    void descontarStockDeInsumoInexistenteDeberiaLanzarExcepcion() {
        when(insumoRepository.buscarConBloqueoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insumoService.descontarStock(99L, new BigDecimal("1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}