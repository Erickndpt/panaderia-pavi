package pe.edu.upeu.panaderia.produccion.orden.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.exception.OrdenProduccionInvalidaException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;
import pe.edu.upeu.panaderia.produccion.orden.dto.DetalleConsumoRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionResponse;
import pe.edu.upeu.panaderia.produccion.orden.entity.DetalleConsumo;
import pe.edu.upeu.panaderia.produccion.orden.entity.EstadoOrdenProduccion;
import pe.edu.upeu.panaderia.produccion.orden.entity.OrdenProduccion;
import pe.edu.upeu.panaderia.produccion.orden.mapper.OrdenProduccionMapper;
import pe.edu.upeu.panaderia.produccion.orden.repository.OrdenProduccionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final OrdenProduccionMapper ordenProduccionMapper;
    private final ProductoService productoService;
    private final InsumoService insumoService;
    private final ExistenciaService existenciaService;

    @Override
    @Transactional
    public OrdenProduccionResponse registrar(OrdenProduccionRequest request) {
        if (request.insumos().isEmpty()) {
            throw new OrdenProduccionInvalidaException(
                    "La orden de producción debe incluir al menos un insumo");
        }
        validarInsumosUnicos(request.insumos());

        ProductoResponse producto = productoService.obtener(request.productoId());

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProductoId(request.productoId());
        orden.setNombreProducto(producto.getNombre());
        orden.setFecha(LocalDateTime.now());
        orden.setEstado(EstadoOrdenProduccion.COMPLETADA);
        orden.setCantidadProducida(request.cantidadProducida());

        for (DetalleConsumoRequest detalleRequest : request.insumos()) {
            InsumoResponse insumo = insumoService.obtener(detalleRequest.insumoId());

            DetalleConsumo detalle = new DetalleConsumo();
            detalle.setInsumoId(detalleRequest.insumoId());
            detalle.setNombreInsumo(insumo.getNombre());
            detalle.setUnidadMedida(insumo.getUnidadMedida());
            detalle.setCantidadConsumida(detalleRequest.cantidadConsumida());
            orden.agregarDetalle(detalle);

            insumoService.descontarStock(detalleRequest.insumoId(), detalleRequest.cantidadConsumida());
        }

        existenciaService.reponer(request.productoId(), request.cantidadProducida());

        OrdenProduccion guardada = ordenProduccionRepository.save(orden);
        log.info("Orden de producción registrada con id {}, producto {}, cantidad producida {} y {} insumo(s)",
                guardada.getId(), guardada.getNombreProducto(), guardada.getCantidadProducida(),
                guardada.getDetalles().size());
        return ordenProduccionMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listar() {
        return ordenProduccionRepository.findAll().stream()
                .map(ordenProduccionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenProduccionResponse obtener(Long id) {
        return ordenProduccionMapper.toResponse(buscarOFallar(id));
    }

    private OrdenProduccion buscarOFallar(Long id) {
        return ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción", id));
    }

    private void validarInsumosUnicos(List<DetalleConsumoRequest> detalles) {
        Set<Long> idsVistos = new HashSet<>();
        for (DetalleConsumoRequest detalle : detalles) {
            if (!idsVistos.add(detalle.insumoId())) {
                throw new OrdenProduccionInvalidaException(
                        "El insumo %d aparece más de una vez en la orden".formatted(detalle.insumoId()));
            }
        }
    }
}