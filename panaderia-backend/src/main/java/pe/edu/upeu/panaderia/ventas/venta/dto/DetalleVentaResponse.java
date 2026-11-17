package pe.edu.upeu.panaderia.ventas.venta.dto;

import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        Integer cantidad,
        BigDecimal subtotal) {
}