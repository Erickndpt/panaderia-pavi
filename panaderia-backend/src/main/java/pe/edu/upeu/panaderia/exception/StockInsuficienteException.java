package pe.edu.upeu.panaderia.exception;

import java.math.BigDecimal;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(Long productoId, BigDecimal cantidadSolicitada, BigDecimal cantidadDisponible) {
        super("Stock insuficiente para el producto %d: se solicitó %s y hay %s disponibles"
                .formatted(productoId, cantidadSolicitada, cantidadDisponible));
    }
}