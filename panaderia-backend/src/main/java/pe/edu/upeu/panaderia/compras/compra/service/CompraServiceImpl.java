package pe.edu.upeu.panaderia.compras.compra.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraRequest;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraResponse;
import pe.edu.upeu.panaderia.compras.compra.dto.DetalleCompraRequest;
import pe.edu.upeu.panaderia.compras.compra.entity.Compra;
import pe.edu.upeu.panaderia.compras.compra.entity.DetalleCompra;
import pe.edu.upeu.panaderia.compras.compra.entity.EstadoCompra;
import pe.edu.upeu.panaderia.compras.compra.mapper.CompraMapper;
import pe.edu.upeu.panaderia.compras.compra.repository.CompraRepository;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;
import pe.edu.upeu.panaderia.compras.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.panaderia.exception.CompraInvalidaException;
import pe.edu.upeu.panaderia.exception.CompraYaAnuladaException;
import pe.edu.upeu.panaderia.exception.ProveedorInactivoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasCompraService;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final CompraMapper compraMapper;
    private final ProveedorRepository proveedorRepository;
    private final InsumoService insumoService;
    private final FinanzasCompraService finanzasService;

    @Override
    @Transactional
    public CompraResponse registrar(CompraRequest request) {
        if (request.detalles().isEmpty()) {
            throw new CompraInvalidaException("La compra debe incluir al menos un detalle");
        }
        Proveedor proveedor = buscarProveedorActivoOFallar(request.proveedorId());

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setFecha(LocalDateTime.now());
        compra.setEstado(EstadoCompra.REGISTRADA);
        compra.setTotal(BigDecimal.ZERO);

        for (DetalleCompraRequest detalleRequest : request.detalles()) {
            InsumoResponse insumo = insumoService.obtener(detalleRequest.insumoId());

            BigDecimal subtotal = detalleRequest.precioUnitario()
                    .multiply(detalleRequest.cantidad())
                    .setScale(2, RoundingMode.HALF_UP);

            DetalleCompra detalle = new DetalleCompra();
            detalle.setInsumoId(detalleRequest.insumoId());
            detalle.setNombreInsumo(insumo.getNombre());
            detalle.setUnidadMedida(insumo.getUnidadMedida());
            detalle.setCantidad(detalleRequest.cantidad());
            detalle.setPrecioUnitario(detalleRequest.precioUnitario());
            detalle.setSubtotal(subtotal);
            compra.agregarDetalle(detalle);
            compra.setTotal(compra.getTotal().add(subtotal));

            insumoService.aumentarStock(detalleRequest.insumoId(), detalleRequest.cantidad());
        }

        Compra guardada = compraRepository.save(compra);
        finanzasService.registrarEgresoCompra(
                guardada.getId(), guardada.getFecha(), guardada.getTotal());
        log.info("Compra registrada con id {}, proveedor {}, total {} y {} detalle(s)",
                guardada.getId(), guardada.getProveedor().getId(), guardada.getTotal(),
                guardada.getDetalles().size());
        return compraMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponse> listar() {
        return compraRepository.findAll().stream()
                .map(compraMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        return compraMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public CompraResponse anular(Long id) {
        Compra compra = compraRepository.buscarConBloqueoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", id));
        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new CompraYaAnuladaException(id);
        }

        BigDecimal totalDescontado = BigDecimal.ZERO;
        for (DetalleCompra detalle : compra.getDetalles()) {
            insumoService.descontarStock(detalle.getInsumoId(), detalle.getCantidad());
            totalDescontado = totalDescontado.add(detalle.getCantidad());
        }

        compra.setEstado(EstadoCompra.ANULADA);
        finanzasService.anularEgresoCompra(id);
        log.info("Compra anulada con id {}, {} detalle(s) y {} cantidad total descontada",
                id, compra.getDetalles().size(), totalDescontado);
        return compraMapper.toResponse(compra);
    }

    private Compra buscarOFallar(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", id));
    }

    private Proveedor buscarProveedorActivoOFallar(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", proveedorId));
        if (Boolean.TRUE.equals(proveedor.getActivo())) {
            return proveedor;
        }
        throw new ProveedorInactivoException(proveedorId);
    }
}
