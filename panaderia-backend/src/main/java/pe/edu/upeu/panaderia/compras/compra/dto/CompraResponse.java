package pe.edu.upeu.panaderia.compras.compra.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.panaderia.compras.compra.entity.EstadoCompra;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResumen;

public record CompraResponse(
        Long id,
        ProveedorResumen proveedor,
        LocalDateTime fecha,
        EstadoCompra estado,
        BigDecimal total,
        List<DetalleCompraResponse> detalles) {
}