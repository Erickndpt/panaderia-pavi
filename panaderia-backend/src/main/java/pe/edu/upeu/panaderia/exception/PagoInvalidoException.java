package pe.edu.upeu.panaderia.exception;

/**
 * El monto del pago no cumple las reglas de validación de negocio ya
 * verificadas en el DTO (mayor que cero).
 *
 * <p>Se mantiene como salvaguarda para invocaciones que no pasen por la
 * validación de bean.
 */
public class PagoInvalidoException extends RuntimeException {

    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}