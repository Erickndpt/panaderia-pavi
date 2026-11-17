package pe.edu.upeu.panaderia.ventas.pago.entity;

/**
 * Métodos de pago admitidos. Debe coincidir con la restricción
 * {@code CK_PAGOS_METODO} de PAN_VENTAS.PAGOS_VENTAS.
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    YAPE,
    PLIN,
    OTRO
}