package pe.edu.upeu.panaderia.exception;

public class ProveedorDuplicadoException extends RuntimeException {

    public ProveedorDuplicadoException(String ruc) {
        super("Ya existe un proveedor con el RUC %s".formatted(ruc));
    }
}