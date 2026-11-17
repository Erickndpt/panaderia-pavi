package pe.edu.upeu.panaderia.compras.proveedor.service;

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
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResponse;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;
import pe.edu.upeu.panaderia.compras.proveedor.mapper.ProveedorMapper;
import pe.edu.upeu.panaderia.compras.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.panaderia.exception.ProveedorDuplicadoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProveedorMapper proveedorMapper;

    @InjectMocks
    private ProveedorServiceImpl proveedorService;

    private ProveedorRequest requestValido() {
        ProveedorRequest request = new ProveedorRequest();
        request.setRuc("20123456789");
        request.setRazonSocial("Molino La Estrella S.A.C.");
        request.setTelefono("987654321");
        request.setEmail("contacto@molinoestrella.pe");
        request.setActivo(true);
        return request;
    }

    private Proveedor proveedorValido() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setRuc("20123456789");
        proveedor.setRazonSocial("Molino La Estrella S.A.C.");
        proveedor.setTelefono("987654321");
        proveedor.setEmail("contacto@molinoestrella.pe");
        proveedor.setActivo(true);
        return proveedor;
    }

    private ProveedorResponse responseValido() {
        return ProveedorResponse.builder()
                .id(1L)
                .ruc("20123456789")
                .razonSocial("Molino La Estrella S.A.C.")
                .telefono("987654321")
                .email("contacto@molinoestrella.pe")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Listar proveedores devuelve todos los registrados")
    void listarDeberiaDevolverProveedores() {
        when(proveedorRepository.findAll()).thenReturn(List.of(proveedorValido()));
        when(proveedorMapper.toResponse(any(Proveedor.class))).thenReturn(responseValido());

        List<ProveedorResponse> responses = proveedorService.listar();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRuc()).isEqualTo("20123456789");
        assertThat(responses.get(0).getRazonSocial()).isEqualTo("Molino La Estrella S.A.C.");
    }

    @Test
    @DisplayName("Obtener proveedor existente devuelve su respuesta")
    void obtenerExistenteDeberiaDevolverProveedor() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedorValido()));
        when(proveedorMapper.toResponse(any(Proveedor.class))).thenReturn(responseValido());

        ProveedorResponse response = proveedorService.obtener(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTelefono()).isEqualTo("987654321");
    }

    @Test
    @DisplayName("Obtener proveedor inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzarExcepcion() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Crear proveedor válido guarda y devuelve la respuesta")
    void crearDeberiaGuardarYDevolverProveedor() {
        when(proveedorRepository.findByRuc("20123456789")).thenReturn(Optional.empty());
        when(proveedorMapper.toEntity(any(ProveedorRequest.class))).thenReturn(proveedorValido());
        when(proveedorRepository.save(any(Proveedor.class))).thenReturn(proveedorValido());
        when(proveedorMapper.toResponse(any(Proveedor.class))).thenReturn(responseValido());

        ProveedorResponse response = proveedorService.crear(requestValido());

        assertThat(response.getRuc()).isEqualTo("20123456789");
        assertThat(response.getActivo()).isTrue();
        verify(proveedorRepository).save(any(Proveedor.class));
    }

    @Test
    @DisplayName("Crear proveedor con RUC duplicado lanza ProveedorDuplicadoException y no guarda")
    void crearConRucDuplicadoDeberiaLanzarSinGuardar() {
        when(proveedorRepository.findByRuc("20123456789")).thenReturn(Optional.of(proveedorValido()));

        assertThatThrownBy(() -> proveedorService.crear(requestValido()))
                .isInstanceOf(ProveedorDuplicadoException.class)
                .hasMessageContaining("20123456789");

        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    @DisplayName("Actualizar proveedor existente conservando su RUC modifica sin conflicto")
    void actualizarDeberiaModificarElProveedorExistente() {
        Proveedor proveedor = proveedorValido();
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findByRuc("20123456789")).thenReturn(Optional.of(proveedor));
        when(proveedorMapper.toResponse(any(Proveedor.class))).thenReturn(responseValido());

        ProveedorResponse response = proveedorService.actualizar(1L, requestValido());

        assertThat(response.getRazonSocial()).isEqualTo("Molino La Estrella S.A.C.");
    }

    @Test
    @DisplayName("Actualizar proveedor con RUC usado por otro proveedor lanza ProveedorDuplicadoException")
    void actualizarConRucDeOtroProveedorDeberiaLanzarExcepcion() {
        Proveedor proveedor = proveedorValido();
        Proveedor otroProveedor = proveedorValido();
        otroProveedor.setId(2L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findByRuc("20123456789")).thenReturn(Optional.of(otroProveedor));

        assertThatThrownBy(() -> proveedorService.actualizar(1L, requestValido()))
                .isInstanceOf(ProveedorDuplicadoException.class)
                .hasMessageContaining("20123456789");
    }

    @Test
    @DisplayName("Actualizar proveedor inexistente lanza ResourceNotFoundException")
    void actualizarInexistenteDeberiaLanzarExcepcion() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorService.actualizar(99L, requestValido()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar proveedor existente borra el registro")
    void eliminarDeberiaBorrarElProveedorExistente() {
        Proveedor proveedor = proveedorValido();
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        proveedorService.eliminar(1L);

        verify(proveedorRepository).delete(proveedor);
    }

    @Test
    @DisplayName("Eliminar proveedor inexistente lanza ResourceNotFoundException y no borra")
    void eliminarInexistenteDeberiaLanzarExcepcion() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(proveedorRepository, never()).delete(any(Proveedor.class));
    }
}