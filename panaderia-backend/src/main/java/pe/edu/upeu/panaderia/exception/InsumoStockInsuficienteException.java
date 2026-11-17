package pe.edu.upeu.panaderia.exception;

import java.math.BigDecimal;

public class InsumoStockInsuficienteException extends RuntimeException {

    public InsumoStockInsuficienteException(Long insumoId, BigDecimal cantidadSolicitada, BigDecimal cantidadDisponible) {
        super("Stock insuficiente para el insumo %d: se solicitó %s y hay %s disponibles"
                .formatted(insumoId, cantidadSolicitada, cantidadDisponible));
    }
}