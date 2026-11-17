package pe.edu.upeu.panaderia.exception;

public class ClienteConVentasException extends RuntimeException {

    public ClienteConVentasException(Long clienteId) {
        super("El cliente con id %d tiene ventas asociadas y no puede eliminarse".formatted(clienteId));
    }
}
