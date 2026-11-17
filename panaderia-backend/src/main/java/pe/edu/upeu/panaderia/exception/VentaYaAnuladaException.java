package pe.edu.upeu.panaderia.exception;

public class VentaYaAnuladaException extends RuntimeException {

    public VentaYaAnuladaException(Long id) {
        super("La venta con id %d ya está anulada".formatted(id));
    }
}