package pe.edu.upeu.panaderia.exception;

public class ExistenciaDuplicadaException extends RuntimeException {

    public ExistenciaDuplicadaException(Long productoId) {
        super("El producto %d ya tiene una existencia registrada".formatted(productoId));
    }
}
