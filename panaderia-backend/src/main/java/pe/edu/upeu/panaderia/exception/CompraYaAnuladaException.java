package pe.edu.upeu.panaderia.exception;

public class CompraYaAnuladaException extends RuntimeException {

    public CompraYaAnuladaException(Long id) {
        super("La compra con id %d ya está anulada".formatted(id));
    }
}