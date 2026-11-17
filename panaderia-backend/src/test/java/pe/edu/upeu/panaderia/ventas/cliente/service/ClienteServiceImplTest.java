package pe.edu.upeu.panaderia.ventas.cliente.service;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.panaderia.exception.ClienteConVentasException;
import pe.edu.upeu.panaderia.exception.ClienteDocumentoDuplicadoException;
import pe.edu.upeu.panaderia.exception.ClienteInactivoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteRequest;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResponse;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;
import pe.edu.upeu.panaderia.ventas.cliente.entity.TipoDocumento;
import pe.edu.upeu.panaderia.ventas.cliente.mapper.ClienteMapper;
import pe.edu.upeu.panaderia.ventas.cliente.repository.ClienteRepository;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRequest requestValido() {
        ClienteRequest request = new ClienteRequest();
        request.setTipoDocumento(TipoDocumento.DNI);
        request.setNumeroDocumento("87654321");
        request.setNombreRazonSocial("Panadería Pavi");
        request.setTelefono("987654321");
        request.setEmail("ventas@panaderiapavi.pe");
        request.setDireccion("Av. Perú 123");
        request.setPais("Perú");
        request.setActivo(true);
        return request;
    }

    private Cliente clienteValido() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("87654321");
        cliente.setNombreRazonSocial("Panadería Pavi");
        cliente.setTelefono("987654321");
        cliente.setEmail("ventas@panaderiapavi.pe");
        cliente.setDireccion("Av. Perú 123");
        cliente.setPais("Perú");
        cliente.setActivo(true);
        return cliente;
    }

    private ClienteResponse responseValido() {
        return ClienteResponse.builder()
                .id(1L)
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("87654321")
                .nombreRazonSocial("Panadería Pavi")
                .telefono("987654321")
                .email("ventas@panaderiapavi.pe")
                .direccion("Av. Perú 123")
                .pais("Perú")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Crear cliente guarda el documento normalizado y devuelve la respuesta")
    void crearDeberiaGuardarYDevolverCliente() {
        ClienteRequest request = requestValido();
        request.setNombreRazonSocial("  Panadería Pavi  ");
        request.setNumeroDocumento("  87654321  ");
        when(clienteRepository.findByNumeroDocumento("87654321")).thenReturn(Optional.empty());
        when(clienteMapper.toEntity(any(ClienteRequest.class))).thenReturn(clienteValido());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = clienteValido();
            cliente.setNombreRazonSocial(request.getNombreRazonSocial());
            cliente.setNumeroDocumento(request.getNumeroDocumento());
            return cliente;
        });
        when(clienteMapper.toResponse(any(Cliente.class))).thenReturn(responseValido());

        ClienteResponse response = clienteService.crear(request);

        assertThat(response.getNombreRazonSocial()).isEqualTo("Panadería Pavi");
        ArgumentCaptor<ClienteRequest> captor = ArgumentCaptor.forClass(ClienteRequest.class);
        verify(clienteMapper).toEntity(captor.capture());
        assertThat(captor.getValue().getNombreRazonSocial()).isEqualTo("Panadería Pavi");
        assertThat(captor.getValue().getNumeroDocumento()).isEqualTo("87654321");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Crear cliente SIN_DOCUMENTO no busca duplicados porque el número queda null")
    void crearSinDocumentoNoDebeBuscarDuplicados() {
        ClienteRequest request = new ClienteRequest();
        request.setTipoDocumento(TipoDocumento.SIN_DOCUMENTO);
        request.setNumeroDocumento("   ");
        request.setNombreRazonSocial("Consumidor final");
        request.setActivo(true);
        when(clienteMapper.toEntity(any(ClienteRequest.class))).thenReturn(clienteValido());
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteValido());
        when(clienteMapper.toResponse(any(Cliente.class))).thenReturn(responseValido());

        clienteService.crear(request);

        assertThat(request.getNumeroDocumento()).isNull();
        verify(clienteRepository, never()).findByNumeroDocumento(any());
    }

    @Test
    @DisplayName("Crear cliente con documento duplicado lanza ClienteDocumentoDuplicadoException y no guarda")
    void crearConDocumentoDuplicadoDeberiaLanzarSinGuardar() {
        when(clienteRepository.findByNumeroDocumento("87654321"))
                .thenReturn(Optional.of(clienteValido()));

        assertThatThrownBy(() -> clienteService.crear(requestValido()))
                .isInstanceOf(ClienteDocumentoDuplicadoException.class)
                .hasMessageContaining("87654321");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Obtener cliente inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzarExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Actualizar cliente conserva su propio documento sin conflicto")
    void actualizarConDocumentoPropioNoDebeLanzarDuplicado() {
        Cliente cliente = clienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByNumeroDocumento("87654321")).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(any(Cliente.class))).thenReturn(responseValido());

        ClienteResponse response = clienteService.actualizar(1L, requestValido());

        assertThat(response.getId()).isEqualTo(1L);
        verify(clienteMapper).actualizar(any(ClienteRequest.class), any(Cliente.class));
    }

    @Test
    @DisplayName("Actualizar cliente con el documento de otro cliente lanza ClienteDocumentoDuplicadoException")
    void actualizarConDocumentoAjenoDeberiaLanzarExcepcion() {
        Cliente cliente = clienteValido();
        Cliente otro = clienteValido();
        otro.setId(2L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByNumeroDocumento("87654321")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> clienteService.actualizar(1L, requestValido()))
                .isInstanceOf(ClienteDocumentoDuplicadoException.class)
                .hasMessageContaining("87654321");

        verify(clienteMapper, never()).actualizar(any(ClienteRequest.class), any(Cliente.class));
    }

    @Test
    @DisplayName("Actualizar cliente inexistente lanza ResourceNotFoundException")
    void actualizarInexistenteDeberiaLanzarExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(99L, requestValido()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar cliente sin ventas borra el registro")
    void eliminarSinVentasDeberiaBorrarElCliente() {
        Cliente cliente = clienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(ventaRepository.countByClienteId(1L)).thenReturn(0L);

        clienteService.eliminar(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    @DisplayName("Eliminar cliente con ventas asociadas lanza ClienteConVentasException y no borra")
    void eliminarConVentasDeberiaLanzarExcepcion() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteValido()));
        when(ventaRepository.countByClienteId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> clienteService.eliminar(1L))
                .isInstanceOf(ClienteConVentasException.class)
                .hasMessageContaining("1");

        verify(clienteRepository, never()).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("Obtener cliente activo devuelve la entidad")
    void obtenerActivoDeberiaDevolverLaEntidad() {
        Cliente cliente = clienteValido();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThat(clienteService.obtenerActivo(1L)).isSameAs(cliente);
    }

    @Test
    @DisplayName("Obtener cliente inactivo lanza ClienteInactivoException")
    void obtenerInactivoDeberiaLanzarExcepcion() {
        Cliente cliente = clienteValido();
        cliente.setActivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> clienteService.obtenerActivo(1L))
                .isInstanceOf(ClienteInactivoException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("Obtener cliente activo inexistente lanza ResourceNotFoundException")
    void obtenerActivoInexistenteDeberiaLanzarExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerActivo(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Listar sin parámetro buscar devuelve todos los clientes")
    void listarSinBuscarDeberiaDevolverTodos() {
        when(clienteRepository.findAll()).thenReturn(List.of(clienteValido()));
        when(clienteMapper.toResponse(any(Cliente.class))).thenReturn(responseValido());

        List<ClienteResponse> responses = clienteService.listar(null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getNumeroDocumento()).isEqualTo("87654321");
        verify(clienteRepository, never()).buscar(any());
    }

    @Test
    @DisplayName("Listar con parámetro buscar delega la búsqueda por nombre o documento")
    void listarConBuscarDeberiaDelegarEnElRepositorio() {
        when(clienteRepository.buscar("pavi")).thenReturn(List.of(clienteValido()));
        when(clienteMapper.toResponse(any(Cliente.class))).thenReturn(responseValido());

        List<ClienteResponse> responses = clienteService.listar("  pavi  ");

        assertThat(responses).hasSize(1);
        verify(clienteRepository).buscar("pavi");
        verify(clienteRepository, never()).findAll();
    }
}
