package pe.edu.upeu.panaderia.ventas.pago.dto;

import java.math.BigDecimal;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPagoVenta;

/**
 * Resumen de cobros de una venta.
 *
 * <p>totalPagado y saldoPendiente solo consideran pagos REGISTRADO; los
 * ANULADO quedan excluidos. Se expone en un endpoint propio para no tocar
 * VentaResponse.
 */
public record PagoVentaResumenResponse(
        Long ventaId,
        BigDecimal totalVenta,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        EstadoPagoVenta estadoPago) {
}