package pe.edu.upeu.panaderia.exception;

public class CompraInvalidaException extends RuntimeException {

    public CompraInvalidaException(String mensaje) {
        super(mensaje);
    }
}