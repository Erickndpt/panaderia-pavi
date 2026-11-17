package pe.edu.upeu.panaderia.exception;

/**
 * El pago superaría el saldo pendiente de la venta.
 *
 * <p>Se traduce a 409: la operación es sintácticamente válida, pero entra en
 * conflicto con el saldo ya registrado.
 */
public class PagoExcedeSaldoException extends RuntimeException {

    public PagoExcedeSaldoException(Long ventaId, java.math.BigDecimal saldoPendiente) {
        super("El pago excede el saldo pendiente de la venta %d (saldo pendiente: %s)"
                .formatted(ventaId, saldoPendiente));
    }
}