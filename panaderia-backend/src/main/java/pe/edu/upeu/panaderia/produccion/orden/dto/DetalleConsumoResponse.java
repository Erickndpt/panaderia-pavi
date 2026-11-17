package pe.edu.upeu.panaderia.produccion.orden.dto;

import java.math.BigDecimal;

public record DetalleConsumoResponse(
        Long id,
        Long insumoId,
        String nombreInsumo,
        String unidadMedida,
        BigDecimal cantidadConsumida) {
}