package pe.edu.upeu.panaderia.exception;

public class MovimientoDuplicadoException extends RuntimeException {

    public MovimientoDuplicadoException(String origen, Long origenId) {
        super("Ya existe un movimiento financiero para la %s %d".formatted(origen, origenId));
    }
}