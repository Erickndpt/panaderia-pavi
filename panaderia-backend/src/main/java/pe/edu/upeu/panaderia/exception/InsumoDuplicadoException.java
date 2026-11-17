package pe.edu.upeu.panaderia.exception;

public class InsumoDuplicadoException extends RuntimeException {

    public InsumoDuplicadoException(String nombre) {
        super("Ya existe un insumo con el nombre %s".formatted(nombre));
    }
}