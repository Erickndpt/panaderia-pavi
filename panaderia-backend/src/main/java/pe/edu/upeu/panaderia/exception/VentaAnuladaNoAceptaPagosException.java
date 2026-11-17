package pe.edu.upeu.panaderia.exception;

/**
 * Se intentó registrar un pago sobre una venta ANULADA.
 *
 * <p>Se traduce a 400 porque la venta no admite pagos en su estado actual.
 */
public class VentaAnuladaNoAceptaPagosException extends RuntimeException {

    public VentaAnuladaNoAceptaPagosException(Long ventaId) {
        super("La venta %d está anulada y no admite pagos".formatted(ventaId));
    }
}