package pe.edu.upeu.panaderia.ventas.pago.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.exception.PagoExcedeSaldoException;
import pe.edu.upeu.panaderia.exception.PagoInvalidoException;
import pe.edu.upeu.panaderia.exception.PagoYaAnuladoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.VentaAnuladaNoAceptaPagosException;
import pe.edu.upeu.panaderia.ventas.AutorizacionVenta;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaRequest;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResponse;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResumenResponse;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPagoVenta;
import pe.edu.upeu.panaderia.ventas.pago.entity.PagoVenta;
import pe.edu.upeu.panaderia.ventas.pago.mapper.PagoVentaMapper;
import pe.edu.upeu.panaderia.ventas.pago.repository.PagoVentaRepository;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoVentaServiceImpl implements PagoVentaService {

    private final PagoVentaRepository pagoVentaRepository;
    private final VentaRepository ventaRepository;
    private final PagoVentaMapper pagoVentaMapper;
    private final AutorizacionVenta autorizacionVenta;

    // El bloqueo pesimista se pide sobre VENTAS, no sobre PAGOS_VENTAS: es
    // la fila de la venta la que serializa a los pagos concurrentes de esa
    // venta. Bloquear solo los pagos no impediría que dos transacciones
    // leyeran el mismo saldo y ambas lo cumplieran.
    @Override
    @Transactional
    public PagoVentaResponse registrar(Long ventaId, PagoVentaRequest request, IdentidadJwt identidad) {
        Venta venta = ventaRepository.buscarConBloqueoPorId(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", ventaId));
        autorizacionVenta.validarPropiedad(venta, identidad);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaAnuladaNoAceptaPagosException(ventaId);
        }
        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PagoInvalidoException("El monto debe ser mayor que cero");
        }

        BigDecimal totalPagado = totalPagado(ventaId);
        BigDecimal saldoPendiente = venta.getTotal().subtract(totalPagado);
        if (request.monto().compareTo(saldoPendiente) > 0) {
            throw new PagoExcedeSaldoException(ventaId, saldoPendiente);
        }

        PagoVenta pago = new PagoVenta();
        pago.setVenta(venta);
        pago.setFecha(LocalDateTime.now());
        pago.setMetodoPago(request.metodoPago());
        pago.setMonto(request.monto());
        pago.setReferencia(normalizarReferencia(request.referencia()));
        pago.setEstado(EstadoPago.REGISTRADO);
        PagoVenta guardado = pagoVentaRepository.save(pago);
        log.info("Pago registrado con id {} para la venta {}", guardado.getId(), ventaId);
        return pagoVentaMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoVentaResponse> listarPorVenta(Long ventaId, IdentidadJwt identidad) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", ventaId));
        autorizacionVenta.validarPropiedad(venta, identidad);
        return pagoVentaRepository.findByVentaIdOrderById(ventaId).stream()
                .map(pagoVentaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagoVentaResponse obtener(Long pagoId, IdentidadJwt identidad) {
        PagoVenta pago = buscarOFallar(pagoId);
        autorizacionVenta.validarPropiedad(pago.getVenta(), identidad);
        return pagoVentaMapper.toResponse(pago);
    }

    // Anular no borra: cambia REGISTRADO por ANULADO y el pago sale del
    // saldo. La segunda anulación se rechaza por conflicto.
    @Override
    @Transactional
    public PagoVentaResponse anular(Long pagoId, IdentidadJwt identidad) {
        PagoVenta pago = buscarOFallar(pagoId);
        autorizacionVenta.validarPuedeAnularPago(identidad);
        if (pago.getEstado() == EstadoPago.ANULADO) {
            throw new PagoYaAnuladoException(pagoId);
        }
        pago.setEstado(EstadoPago.ANULADO);
        log.info("Pago {} anulado", pagoId);
        return pagoVentaMapper.toResponse(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoVentaResumenResponse resumen(Long ventaId, IdentidadJwt identidad) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", ventaId));
        autorizacionVenta.validarPropiedad(venta, identidad);
        BigDecimal totalVenta = venta.getTotal();
        BigDecimal totalPagado = totalPagado(ventaId);
        BigDecimal saldoPendiente = totalVenta.subtract(totalPagado);
        return new PagoVentaResumenResponse(ventaId, totalVenta, totalPagado, saldoPendiente,
                estadoPago(totalPagado, saldoPendiente));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tienePagosRegistrados(Long ventaId) {
        return pagoVentaRepository.existePagadoPorVentaYEstado(ventaId, EstadoPago.REGISTRADO);
    }

    private BigDecimal totalPagado(Long ventaId) {
        BigDecimal suma = pagoVentaRepository.sumarMontosPorVentaYEstado(ventaId, EstadoPago.REGISTRADO);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    private EstadoPagoVenta estadoPago(BigDecimal totalPagado, BigDecimal saldoPendiente) {
        if (totalPagado.compareTo(BigDecimal.ZERO) == 0) {
            return EstadoPagoVenta.PENDIENTE;
        }
        if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            return EstadoPagoVenta.PAGADO;
        }
        return EstadoPagoVenta.PARCIAL;
    }

    private PagoVenta buscarOFallar(Long pagoId) {
        return pagoVentaRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", pagoId));
    }

    // Una referencia en blanco se guarda como NULL: REFERENCIA no es UNIQUE,
    // pero NULL documenta mejor "sin referencia" que una cadena vacía.
    private String normalizarReferencia(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return referencia.trim();
    }
}