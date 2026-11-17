package pe.edu.upeu.panaderia.exception;

public class VentaInvalidaException extends RuntimeException {

    public VentaInvalidaException(String mensaje) {
        super(mensaje);
    }
}