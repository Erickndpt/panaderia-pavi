package pe.edu.upeu.panaderia.exception;

public class ProveedorInactivoException extends RuntimeException {

    public ProveedorInactivoException(Long proveedorId) {
        super("El proveedor con id %d está inactivo".formatted(proveedorId));
    }
}