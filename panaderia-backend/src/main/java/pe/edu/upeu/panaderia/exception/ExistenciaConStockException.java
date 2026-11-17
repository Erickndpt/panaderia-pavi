package pe.edu.upeu.panaderia.exception;

import java.math.BigDecimal;

public class ExistenciaConStockException extends RuntimeException {

    public ExistenciaConStockException(Long productoId, BigDecimal cantidad) {
        super(("No se puede eliminar la existencia del producto %d porque tiene %s disponibles. "
                + "Solo se permite eliminar existencias con cantidad cero")
                .formatted(productoId, cantidad.toPlainString()));
    }
}
