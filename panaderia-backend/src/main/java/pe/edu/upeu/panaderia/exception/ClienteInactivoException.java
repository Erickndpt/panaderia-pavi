package pe.edu.upeu.panaderia.exception;

public class ClienteInactivoException extends RuntimeException {

    public ClienteInactivoException(Long clienteId) {
        super("El cliente con id %d está inactivo".formatted(clienteId));
    }
}
