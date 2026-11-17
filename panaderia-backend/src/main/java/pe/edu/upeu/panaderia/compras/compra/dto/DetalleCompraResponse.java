package pe.edu.upeu.panaderia.compras.compra.dto;

import java.math.BigDecimal;

public record DetalleCompraResponse(
        Long id,
        Long insumoId,
        String nombreInsumo,
        String unidadMedida,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {
}