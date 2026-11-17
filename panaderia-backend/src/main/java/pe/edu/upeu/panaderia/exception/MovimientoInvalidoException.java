package pe.edu.upeu.panaderia.exception;

public class MovimientoInvalidoException extends RuntimeException {

    public MovimientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}