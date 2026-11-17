package pe.edu.upeu.panaderia.exception;

/**
 * La venta tiene al menos un pago REGISTRADO, por lo que no puede anularse.
 *
 * <p>Anular la venta dejaría esos pagoscobrando por una venta que ya no
 * existe. El pago debe anularse primero.
 */
public class VentaConPagosRegistradosException extends RuntimeException {

    public VentaConPagosRegistradosException(Long ventaId) {
        super("La venta %d tiene pagos registrados y no puede anularse; anule los pagos primero"
                .formatted(ventaId));
    }
}