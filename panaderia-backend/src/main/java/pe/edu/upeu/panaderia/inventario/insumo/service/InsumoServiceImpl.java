package pe.edu.upeu.panaderia.inventario.insumo.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.exception.InsumoDuplicadoException;
import pe.edu.upeu.panaderia.exception.InsumoStockInsuficienteException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoRequest;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.entity.Insumo;
import pe.edu.upeu.panaderia.inventario.insumo.mapper.InsumoMapper;
import pe.edu.upeu.panaderia.inventario.insumo.repository.InsumoRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsumoServiceImpl implements InsumoService {

    private final InsumoRepository insumoRepository;
    private final InsumoMapper insumoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InsumoResponse> listar() {
        return insumoRepository.findAll().stream()
                .map(insumoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InsumoResponse obtener(Long id) {
        return insumoMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public InsumoResponse crear(InsumoRequest request) {
        validarNombreDisponible(request.getNombre());
        Insumo insumo = insumoRepository.save(insumoMapper.toEntity(request));
        log.info("Insumo creado con id {}", insumo.getId());
        return insumoMapper.toResponse(insumo);
    }

    @Override
    @Transactional
    public InsumoResponse actualizar(Long id, InsumoRequest request) {
        Insumo insumo = buscarOFallar(id);
        validarNombreDisponibleExcluyendo(id, request.getNombre());
        insumoMapper.actualizar(request, insumo);
        log.info("Insumo actualizado con id {}", id);
        return insumoMapper.toResponse(insumo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Insumo insumo = buscarOFallar(id);
        insumoRepository.delete(insumo);
        log.info("Insumo eliminado con id {}", id);
    }

    @Override
    @Transactional
    public void aumentarStock(Long insumoId, BigDecimal cantidad) {
        validarCantidad(insumoId, cantidad);
        Insumo insumo = insumoRepository.buscarConBloqueoPorId(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo", insumoId));
        BigDecimal stockAnterior = insumo.getStockActual();
        insumo.setStockActual(stockAnterior.add(cantidad));
        log.info("Stock aumentado para el insumo {}: anterior {}, aumentado {}, final {}",
                insumoId, stockAnterior, cantidad, insumo.getStockActual());
    }

    @Override
    @Transactional
    public void descontarStock(Long insumoId, BigDecimal cantidad) {
        validarCantidadDescontar(insumoId, cantidad);
        Insumo insumo = insumoRepository.buscarConBloqueoPorId(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo", insumoId));
        BigDecimal stockAnterior = insumo.getStockActual();
        validarStock(insumoId, cantidad, stockAnterior);
        insumo.setStockActual(stockAnterior.subtract(cantidad));
        log.info("Stock descontado para el insumo {}: anterior {}, descontado {}, final {}",
                insumoId, stockAnterior, cantidad, insumo.getStockActual());
    }

    private Insumo buscarOFallar(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo", id));
    }

    private void validarCantidad(Long insumoId, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad a aumentar para el insumo %d debe ser mayor que cero".formatted(insumoId));
        }
    }

    private void validarCantidadDescontar(Long insumoId, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad a descontar para el insumo %d debe ser mayor que cero".formatted(insumoId));
        }
    }

    private void validarStock(Long insumoId, BigDecimal cantidad, BigDecimal disponible) {
        if (disponible.compareTo(cantidad) < 0) {
            throw new InsumoStockInsuficienteException(insumoId, cantidad, disponible);
        }
    }

    private void validarNombreDisponible(String nombre) {
        if (insumoRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new InsumoDuplicadoException(nombre);
        }
    }

    private void validarNombreDisponibleExcluyendo(Long id, String nombre) {
        insumoRepository.findByNombreIgnoreCase(nombre)
                .filter(insumo -> !insumo.getId().equals(id))
                .ifPresent(insumo -> {
                    throw new InsumoDuplicadoException(nombre);
                });
    }
}