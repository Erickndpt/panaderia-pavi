package pe.edu.upeu.panaderia.inventario.existencia.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExistenciaServiceImpl implements ExistenciaService {

    private final ExistenciaRepository existenciaRepository;
    private final ExistenciaMapper existenciaMapper;
    private final ProductoService productoService;

    @Override
    @Transactional(readOnly = true)
    public List<ExistenciaResponse> listar(Long productoId) {
        if (productoId == null) {
            return existenciaRepository.findAll().stream()
                    .map(this::enriquecer)
                    .toList();
        }

        validarProductoIdPositivo(productoId);
        return existenciaRepository.findAllByIdProductoOrderByIdAsc(productoId).stream()
                .map(this::enriquecer)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExistenciaResponse obtener(Long id) {
        return enriquecer(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ExistenciaResponse obtenerPorProducto(Long productoId) {
        return enriquecer(buscarPorProductoOFallar(productoId));
    }

    @Override
    @Transactional
    public ExistenciaResponse crear(ExistenciaRequest request) {
        ProductoResponse producto = productoService.obtener(request.getProductoId());
        if (existenciaRepository.existsByIdProducto(request.getProductoId())) {
            throw new ExistenciaDuplicadaException(request.getProductoId());
        }

        Existencia existencia = new Existencia();
        existencia.setIdProducto(producto.getId());
        existencia.setCantidad(request.getCantidad());
        Existencia guardada = existenciaRepository.save(existencia);
        log.info("Existencia creada con id {} para el producto {}", guardada.getId(), guardada.getIdProducto());
        return existenciaMapper.toResponse(guardada, producto);
    }

    @Override
    @Transactional
    public ExistenciaResponse actualizar(Long id, ExistenciaRequest request) {
        Existencia existencia = buscarOFallar(id);
        ProductoResponse producto = productoService.obtener(request.getProductoId());

        if (!request.getProductoId().equals(existencia.getIdProducto())
                && existenciaRepository.existsByIdProducto(request.getProductoId())) {
            throw new ExistenciaDuplicadaException(request.getProductoId());
        }

        existencia.setIdProducto(producto.getId());
        existencia.setCantidad(request.getCantidad());
        log.info("Existencia actualizada con id {} para el producto {}", id, producto.getId());
        return existenciaMapper.toResponse(existencia, producto);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Existencia existencia = buscarOFallar(id);
        BigDecimal cantidad = existencia.getCantidad();
        if (cantidad.compareTo(BigDecimal.ZERO) > 0) {
            throw new ExistenciaConStockException(existencia.getIdProducto(), cantidad);
        }

        existenciaRepository.delete(existencia);
        log.info("Existencia eliminada con id {} del producto {}", id, existencia.getIdProducto());
    }

    @Override
    @Transactional
    public void descontar(Long productoId, BigDecimal cantidad) {
        Existencia existencia = buscarConBloqueoOFallar(productoId);
        validarCantidad(productoId, cantidad);
        BigDecimal cantidadAnterior = existencia.getCantidad();
        validarStock(productoId, cantidad, cantidadAnterior);
        existencia.setCantidad(cantidadAnterior.subtract(cantidad));
        log.info("Stock descontado para el producto {}: anterior {}, descontado {}, final {}",
                productoId, cantidadAnterior, cantidad, existencia.getCantidad());
    }

    @Override
    @Transactional
    public void reponer(Long productoId, BigDecimal cantidad) {
        Existencia existencia = buscarConBloqueoOFallar(productoId);
        validarCantidadReponer(productoId, cantidad);
        BigDecimal cantidadAnterior = existencia.getCantidad();
        existencia.setCantidad(cantidadAnterior.add(cantidad));
        log.info("Stock repuesto para el producto {}: anterior {}, repuesto {}, final {}",
                productoId, cantidadAnterior, cantidad, existencia.getCantidad());
    }

    private ExistenciaResponse enriquecer(Existencia existencia) {
        ProductoResponse producto = productoService.obtener(existencia.getIdProducto());
        return existenciaMapper.toResponse(existencia, producto);
    }

    private Existencia buscarOFallar(Long id) {
        return existenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Existencia", id));
    }

    private Existencia buscarPorProductoOFallar(Long productoId) {
        return existenciaRepository.findByIdProducto(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Existencia del producto", productoId));
    }

    private Existencia buscarConBloqueoOFallar(Long productoId) {
        return existenciaRepository.buscarConBloqueoPorProducto(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Existencia del producto", productoId));
    }

    private void validarProductoIdPositivo(Long productoId) {
        if (productoId <= 0) {
            throw new FiltroInvalidoException("productoId debe ser un número positivo");
        }
    }

    private void validarCantidad(Long productoId, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad a descontar para el producto %d debe ser mayor que cero".formatted(productoId));
        }
    }

    private void validarCantidadReponer(Long productoId, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad a reponer para el producto %d debe ser mayor que cero".formatted(productoId));
        }
    }

    private void validarStock(Long productoId, BigDecimal cantidad, BigDecimal disponible) {
        if (disponible.compareTo(cantidad) < 0) {
            throw new StockInsuficienteException(productoId, cantidad, disponible);
        }
    }
}
