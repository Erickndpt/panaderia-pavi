package pe.edu.upeu.panaderia.ventas.pago.entity;

/**
 * Estado de un pago.
 *
 * <p>Ojo: a diferencia de {@code EstadoVenta} (REGISTRADA/ANULADA), el pago
 * usa REGISTRADO/ANULADO, tal y como define {@code CK_PAGOS_ESTADO}. La
 * diferencia es de concordancia: "pago" es masculino, mientras que "venta" es
 * femenino. Solo REGISTRADO suma para el saldo de la venta.
 */
public enum EstadoPago {
    REGISTRADO,
    ANULADO
}