package pe.edu.upeu.panaderia.compras.proveedor.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResponse;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;
import pe.edu.upeu.panaderia.compras.proveedor.mapper.ProveedorMapper;
import pe.edu.upeu.panaderia.compras.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.panaderia.exception.ProveedorDuplicadoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll().stream()
                .map(proveedorMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return proveedorMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        validarRucDisponible(request.getRuc());
        Proveedor proveedor = proveedorRepository.save(proveedorMapper.toEntity(request));
        log.info("Proveedor creado con id {}", proveedor.getId());
        return proveedorMapper.toResponse(proveedor);
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscarOFallar(id);
        validarRucDisponibleExcluyendo(id, request.getRuc());
        proveedorMapper.actualizar(request, proveedor);
        log.info("Proveedor actualizado con id {}", id);
        return proveedorMapper.toResponse(proveedor);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = buscarOFallar(id);
        proveedorRepository.delete(proveedor);
        log.info("Proveedor eliminado con id {}", id);
    }

    private Proveedor buscarOFallar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
    }

    private void validarRucDisponible(String ruc) {
        if (proveedorRepository.findByRuc(ruc).isPresent()) {
            throw new ProveedorDuplicadoException(ruc);
        }
    }

    private void validarRucDisponibleExcluyendo(Long id, String ruc) {
        proveedorRepository.findByRuc(ruc)
                .filter(proveedor -> !proveedor.getId().equals(id))
                .ifPresent(proveedor -> {
                    throw new ProveedorDuplicadoException(ruc);
                });
    }
}