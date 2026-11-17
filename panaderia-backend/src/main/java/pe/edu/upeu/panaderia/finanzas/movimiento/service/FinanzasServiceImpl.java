package pe.edu.upeu.panaderia.finanzas.movimiento.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.exception.MovimientoDuplicadoException;
import pe.edu.upeu.panaderia.exception.MovimientoInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.MovimientoFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.ResumenFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.EstadoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.MovimientoFinanciero;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.OrigenMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.TipoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.mapper.MovimientoFinancieroMapper;
import pe.edu.upeu.panaderia.finanzas.movimiento.repository.MovimientoFinancieroRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanzasServiceImpl
        implements FinanzasService, FinanzasVentaService, FinanzasCompraService {

    private final MovimientoFinancieroRepository movimientoRepository;
    private final MovimientoFinancieroMapper movimientoMapper;

    @Override
    @Transactional
    public void registrarIngresoVenta(
            Long ventaId, LocalDateTime fecha, String tipoVenta, BigDecimal monto) {
        validarOrigenNoDuplicado(OrigenMovimiento.VENTA, ventaId);
        validarMonto(monto);
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setFecha(fecha);
        movimiento.setTipo(TipoMovimiento.INGRESO);
        movimiento.setOrigen(OrigenMovimiento.VENTA);
        movimiento.setOrigenId(ventaId);
        movimiento.setConcepto("Ingreso por venta %d (%s)".formatted(ventaId, tipoVenta));
        movimiento.setMonto(monto);
        movimiento.setEstado(EstadoMovimiento.ACTIVO);
        MovimientoFinanciero guardado = movimientoRepository.save(movimiento);
        log.info("Ingreso por la venta {} registrado como movimiento {}",
                ventaId, guardado.getId());
    }

    @Override
    @Transactional
    public void registrarEgresoCompra(
            Long compraId, LocalDateTime fecha, BigDecimal monto) {
        validarOrigenNoDuplicado(OrigenMovimiento.COMPRA, compraId);
        validarMonto(monto);
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setFecha(fecha);
        movimiento.setTipo(TipoMovimiento.EGRESO);
        movimiento.setOrigen(OrigenMovimiento.COMPRA);
        movimiento.setOrigenId(compraId);
        movimiento.setConcepto("Egreso por compra %d".formatted(compraId));
        movimiento.setMonto(monto);
        movimiento.setEstado(EstadoMovimiento.ACTIVO);
        MovimientoFinanciero guardado = movimientoRepository.save(movimiento);
        log.info("Egreso por la compra {} registrado como movimiento {}",
                compraId, guardado.getId());
    }

    @Override
    @Transactional
    public void anularIngresoVenta(Long ventaId) {
        MovimientoFinanciero movimiento = movimientoRepository.findByOrigenAndOrigenId(
                        OrigenMovimiento.VENTA, ventaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Movimiento financiero de la venta", ventaId));
        if (movimiento.getEstado() == EstadoMovimiento.ANULADO) {
            throw new MovimientoInvalidoException(
                    "El ingreso de la venta %d ya está anulado".formatted(ventaId));
        }
        movimiento.setEstado(EstadoMovimiento.ANULADO);
        log.info("Ingreso de la venta {} anulado (movimiento {})",
                ventaId, movimiento.getId());
    }

    @Override
    @Transactional
    public void anularEgresoCompra(Long compraId) {
        MovimientoFinanciero movimiento = movimientoRepository.findByOrigenAndOrigenId(
                        OrigenMovimiento.COMPRA, compraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Movimiento financiero de la compra", compraId));
        if (movimiento.getEstado() == EstadoMovimiento.ANULADO) {
            throw new MovimientoInvalidoException(
                    "El egreso de la compra %d ya está anulado".formatted(compraId));
        }
        movimiento.setEstado(EstadoMovimiento.ANULADO);
        log.info("Egreso de la compra {} anulado (movimiento {})",
                compraId, movimiento.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoFinancieroResponse> listar() {
        return movimientoRepository.findAll().stream()
                .map(movimientoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MovimientoFinancieroResponse obtener(Long id) {
        return movimientoMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenFinancieroResponse resumen() {
        long cantidadIngresos = movimientoRepository.contarPorTipoYEstado(
                TipoMovimiento.INGRESO, EstadoMovimiento.ACTIVO);
        BigDecimal montoIngresos = movimientoRepository.sumarMontoPorTipoYEstado(
                TipoMovimiento.INGRESO, EstadoMovimiento.ACTIVO);
        long cantidadEgresos = movimientoRepository.contarPorTipoYEstado(
                TipoMovimiento.EGRESO, EstadoMovimiento.ACTIVO);
        BigDecimal montoEgresos = movimientoRepository.sumarMontoPorTipoYEstado(
                TipoMovimiento.EGRESO, EstadoMovimiento.ACTIVO);
        BigDecimal saldo = montoIngresos.subtract(montoEgresos);
        return new ResumenFinancieroResponse(
                cantidadIngresos, montoIngresos, cantidadEgresos, montoEgresos, saldo);
    }

    private void validarOrigenNoDuplicado(OrigenMovimiento origen, Long origenId) {
        if (movimientoRepository.findByOrigenAndOrigenId(origen, origenId).isPresent()) {
            throw new MovimientoDuplicadoException(origen.name().toLowerCase(), origenId);
        }
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new MovimientoInvalidoException("El monto debe ser mayor o igual a cero");
        }
    }

    private MovimientoFinanciero buscarOFallar(Long id) {
        return movimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento financiero", id));
    }
}
