package pe.edu.upeu.panaderia.exception;

public class ClienteDocumentoDuplicadoException extends RuntimeException {

    public ClienteDocumentoDuplicadoException(String numeroDocumento) {
        super("Ya existe un cliente con el número de documento %s".formatted(numeroDocumento));
    }
}
