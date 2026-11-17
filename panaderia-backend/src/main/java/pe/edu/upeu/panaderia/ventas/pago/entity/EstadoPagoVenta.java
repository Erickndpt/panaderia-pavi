package pe.edu.upeu.panaderia.ventas.pago.entity;

/**
 * Situación de la venta frente a lo pagado. Es un valor derivado, no una
 * columna: se calcula a partir del total y de la suma de pagos REGISTRADO.
 */
public enum EstadoPagoVenta {
    PENDIENTE,
    PARCIAL,
    PAGADO
}