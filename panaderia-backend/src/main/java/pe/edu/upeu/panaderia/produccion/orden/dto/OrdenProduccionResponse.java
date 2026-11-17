package pe.edu.upeu.panaderia.produccion.orden.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.panaderia.produccion.orden.entity.EstadoOrdenProduccion;

public record OrdenProduccionResponse(
        Long id,
        LocalDateTime fecha,
        EstadoOrdenProduccion estado,
        Long productoId,
        String nombreProducto,
        BigDecimal cantidadProducida,
        List<DetalleConsumoResponse> detalles) {
}