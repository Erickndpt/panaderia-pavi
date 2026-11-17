package pe.edu.upeu.panaderia.finanzas.movimiento.dto;

import java.math.BigDecimal;

public record ResumenFinancieroResponse(
        long cantidadIngresos,
        BigDecimal montoIngresos,
        long cantidadEgresos,
        BigDecimal montoEgresos,
        BigDecimal saldo) {
}