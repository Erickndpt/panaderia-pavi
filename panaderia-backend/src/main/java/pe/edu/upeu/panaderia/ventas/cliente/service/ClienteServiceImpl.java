package pe.edu.upeu.panaderia.ventas.cliente.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.exception.ClienteConVentasException;
import pe.edu.upeu.panaderia.exception.ClienteDocumentoDuplicadoException;
import pe.edu.upeu.panaderia.exception.ClienteInactivoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteRequest;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResponse;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;
import pe.edu.upeu.panaderia.ventas.cliente.mapper.ClienteMapper;
import pe.edu.upeu.panaderia.ventas.cliente.repository.ClienteRepository;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final VentaRepository ventaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String buscar) {
        List<Cliente> clientes = buscar == null || buscar.isBlank()
                ? clienteRepository.findAll()
                : clienteRepository.buscar(buscar.trim());
        return clientes.stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return clienteMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        normalizar(request);
        validarDocumentoDisponible(request.getNumeroDocumento());
        Cliente cliente = clienteRepository.save(clienteMapper.toEntity(request));
        log.info("Cliente creado con id {} y tipo de documento {}", cliente.getId(), cliente.getTipoDocumento());
        return clienteMapper.toResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarOFallar(id);
        normalizar(request);
        validarDocumentoDisponibleExcluyendo(id, request.getNumeroDocumento());
        clienteMapper.actualizar(request, cliente);
        log.info("Cliente actualizado con id {}", id);
        return clienteMapper.toResponse(cliente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarOFallar(id);
        // Las ventas, incluidas las anuladas, se conservan: se bloquea la
        // eliminación en lugar de borrar el histórico transaccional.
        if (ventaRepository.countByClienteId(id) > 0) {
            throw new ClienteConVentasException(id);
        }
        clienteRepository.delete(cliente);
        log.info("Cliente eliminado con id {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerActivo(Long id) {
        Cliente cliente = buscarOFallar(id);
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException(id);
        }
        return cliente;
    }

    private Cliente buscarOFallar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    private void validarDocumentoDisponible(String numeroDocumento) {
        if (numeroDocumento == null) {
            return;
        }
        if (clienteRepository.findByNumeroDocumento(numeroDocumento).isPresent()) {
            throw new ClienteDocumentoDuplicadoException(numeroDocumento);
        }
    }

    private void validarDocumentoDisponibleExcluyendo(Long id, String numeroDocumento) {
        if (numeroDocumento == null) {
            return;
        }
        clienteRepository.findByNumeroDocumento(numeroDocumento)
                .filter(cliente -> !cliente.getId().equals(id))
                .ifPresent(cliente -> {
                    throw new ClienteDocumentoDuplicadoException(numeroDocumento);
                });
    }

    private void normalizar(ClienteRequest request) {
        request.setNombreRazonSocial(textoNormalizado(request.getNombreRazonSocial()));
        request.setNumeroDocumento(textoNormalizado(request.getNumeroDocumento()));
        request.setTelefono(textoNormalizado(request.getTelefono()));
        request.setEmail(textoNormalizado(request.getEmail()));
        request.setDireccion(textoNormalizado(request.getDireccion()));
        request.setPais(textoNormalizado(request.getPais()));
    }

    private String textoNormalizado(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
