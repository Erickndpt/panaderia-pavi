package pe.edu.upeu.panaderia.exception;

/**
 * El pago ya está ANULADO y no admite una segunda anulación.
 */
public class PagoYaAnuladoException extends RuntimeException {

    public PagoYaAnuladoException(Long id) {
        super("El pago %d ya está anulado".formatted(id));
    }
}