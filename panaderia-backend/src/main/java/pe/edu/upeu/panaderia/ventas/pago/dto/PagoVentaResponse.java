package pe.edu.upeu.panaderia.ventas.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.MetodoPago;

/**
 * Salida de pago.
 *
 * <p>La respuesta de venta sigue igual que antes: los pagos no se añaden
 * a VentaResponse para no romper el contrato existente.
 */
public record PagoVentaResponse(
        Long id,
        Long ventaId,
        LocalDateTime fecha,
        MetodoPago metodoPago,
        BigDecimal monto,
        String referencia,
        EstadoPago estado) {
}