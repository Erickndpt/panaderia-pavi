package pe.edu.upeu.panaderia.ventas.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.service.ProductoService;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.VentaInvalidaException;
import pe.edu.upeu.panaderia.exception.VentaConPagosRegistradosException;
import pe.edu.upeu.panaderia.exception.VentaYaAnuladaException;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasVentaService;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;
import pe.edu.upeu.panaderia.ventas.AutorizacionVenta;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;
import pe.edu.upeu.panaderia.ventas.cliente.service.ClienteService;
import pe.edu.upeu.panaderia.ventas.pago.service.PagoVentaService;
import pe.edu.upeu.panaderia.ventas.venta.dto.DetalleVentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.DetalleVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;
import pe.edu.upeu.panaderia.ventas.venta.mapper.VentaMapper;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;
import pe.edu.upeu.panaderia.ventas.venta.specification.VentaSpecifications;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final VentaMapper ventaMapper;
    private final ProductoService productoService;
    private final ExistenciaService existenciaService;
    private final FinanzasVentaService finanzasService;
    private final ClienteService clienteService;
    private final PagoVentaService pagoVentaService;
    private final AutorizacionVenta autorizacionVenta;

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> listar(TipoVenta tipo, EstadoVenta estado, LocalDateTime fechaDesde,
                                      LocalDateTime fechaHasta, String paisDestino, Long productoId,
                                      Long clienteId, String ordenFecha, IdentidadJwt identidad) {
        validarRangoFechas(fechaDesde, fechaHasta);
        Sort orden = construirOrden(ordenFecha);
        Specification<Venta> filtros = construirFiltros(tipo, estado, fechaDesde, fechaHasta, paisDestino, productoId,
                clienteId, vendedorIdSiEsVendedor(identidad));
        List<Venta> ventas = filtros == null
                ? ventaRepository.findAll(orden)
                : ventaRepository.findAll(filtros, orden);
        return ventas.stream()
                .map(ventaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id, IdentidadJwt identidad) {
        Venta venta = buscarOFallar(id);
        autorizacionVenta.validarPropiedad(venta, identidad);
        return ventaMapper.toResponse(venta);
    }

    @Override
    @Transactional
    public VentaResponse registrar(VentaRequest request, IdentidadJwt identidad) {
        String pais = normalizarPais(request);
        validarTipoYPais(request.tipo(), pais, request.detalles());
        // El cliente se resuelve antes de descontar existencias: si no existe
        // (404) o está inactivo (409) la venta no llega a tocar el stock.
        Cliente cliente = clienteService.obtenerActivo(request.clienteId());

        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setCliente(cliente);
        venta.setTipo(request.tipo());
        venta.setPaisDestino(pais);
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setTotal(BigDecimal.ZERO);
        venta.setVendedorId(identidad.id());
        venta.setVendedorUsername(identidad.username());
        venta.setVendedorNombre(identidad.nombre());

        for (DetalleVentaRequest detalleRequest : request.detalles()) {
            ProductoResponse producto = productoService.obtener(detalleRequest.productoId());
            existenciaService.descontar(detalleRequest.productoId(), BigDecimal.valueOf(detalleRequest.cantidad()));

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(detalleRequest.cantidad()))
                    .setScale(2, RoundingMode.HALF_UP);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setIdProducto(detalleRequest.productoId());
            detalle.setNombreProducto(producto.getNombre());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setCantidad(detalleRequest.cantidad());
            detalle.setSubtotal(subtotal);
            venta.agregarDetalle(detalle);
            venta.setTotal(venta.getTotal().add(subtotal));
        }

        Venta guardada = ventaRepository.save(venta);
        finanzasService.registrarIngresoVenta(
                guardada.getId(), guardada.getFecha(), guardada.getTipo().name(), guardada.getTotal());
        log.info("Venta registrada con id {}, cliente {}, tipo {}, total {} y {} detalle(s)",
                guardada.getId(), cliente.getId(), guardada.getTipo(), guardada.getTotal(),
                guardada.getDetalles().size());
        return ventaMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id) {
        Venta venta = ventaRepository.buscarConBloqueoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", id));
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException(id);
        }
        // Comprobación antes de reponer stock y de tocar finanzas: anular una
        // venta con cobros vivos dejaría pagos REGISTRADO huérfanos. Los pagos
        // ANULADO no bloquean la anulación.
        if (pagoVentaService.tienePagosRegistrados(id)) {
            throw new VentaConPagosRegistradosException(id);
        }
        BigDecimal totalRepuesto = BigDecimal.ZERO;
        for (DetalleVenta detalle : venta.getDetalles()) {
            BigDecimal cantidadRepuesta = BigDecimal.valueOf(detalle.getCantidad());
            existenciaService.reponer(detalle.getIdProducto(), cantidadRepuesta);
            totalRepuesto = totalRepuesto.add(cantidadRepuesta);
        }
        venta.setEstado(EstadoVenta.ANULADA);
        finanzasService.anularIngresoVenta(id);
        log.info("Venta anulada con id {}, {} detalle(s), cantidades repuestas {}",
                id, venta.getDetalles().size(), totalRepuesto);
        return ventaMapper.toResponse(venta);
    }

    private Venta buscarOFallar(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", id));
    }

    private String vendedorIdSiEsVendedor(IdentidadJwt identidad) {
        return identidad != null && identidad.vendedor() ? identidad.id() : null;
    }

    private String normalizarPais(VentaRequest request) {
        if (request.paisDestino() == null) {
            return null;
        }
        String pais = request.paisDestino().trim();
        return pais.isEmpty() ? null : pais;
    }

    private Specification<Venta> construirFiltros(TipoVenta tipo, EstadoVenta estado, LocalDateTime fechaDesde,
                                                  LocalDateTime fechaHasta, String paisDestino, Long productoId,
                                                  Long clienteId, String vendedorId) {
        List<Specification<Venta>> predicados = new ArrayList<>();
        if (tipo != null) {
            predicados.add(VentaSpecifications.tipoIgual(tipo));
        }
        if (estado != null) {
            predicados.add(VentaSpecifications.estadoIgual(estado));
        }
        if (fechaDesde != null) {
            predicados.add(VentaSpecifications.fechaMayorIgual(fechaDesde));
        }
        if (fechaHasta != null) {
            predicados.add(VentaSpecifications.fechaMenorIgual(fechaHasta));
        }
        if (paisDestino != null && !paisDestino.isBlank()) {
            predicados.add(VentaSpecifications.paisDestinoContiene(paisDestino));
        }
        if (productoId != null) {
            predicados.add(VentaSpecifications.contieneProductoId(productoId));
        }
        if (clienteId != null) {
            predicados.add(VentaSpecifications.clienteIgual(clienteId));
        }
        if (vendedorId != null) {
            predicados.add(VentaSpecifications.vendedorIgual(vendedorId));
        }
        return predicados.isEmpty() ? null : Specification.allOf(predicados);
    }


    private Sort construirOrden(String ordenFecha) {
        String direccionTexto = ordenFecha == null ? "DESC" : ordenFecha.trim();
        Sort.Direction direccion = switch (direccionTexto.toUpperCase(Locale.ROOT)) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default -> throw new FiltroInvalidoException("ordenFecha debe ser ASC o DESC");
        };
        return Sort.by(direccion, "fecha");
    }

    private void validarRangoFechas(LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new FiltroInvalidoException("fechaDesde no debe ser posterior a fechaHasta");
        }
    }

    private void validarTipoYPais(TipoVenta tipo, String pais, List<DetalleVentaRequest> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new VentaInvalidaException("La venta debe incluir al menos un detalle");
        }
        if (tipo == null) {
            throw new VentaInvalidaException("El tipo de venta es obligatorio");
        }
        if (tipo == TipoVenta.LOCAL && pais != null) {
            throw new VentaInvalidaException("Una venta LOCAL no debe tener país de destino");
        }
        if (tipo == TipoVenta.EXPORTACION && (pais == null || pais.isBlank())) {
            throw new VentaInvalidaException("Una venta EXPORTACION exige un país de destino");
        }
    }
}
