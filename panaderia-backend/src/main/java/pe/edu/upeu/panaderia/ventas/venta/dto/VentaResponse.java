package pe.edu.upeu.panaderia.ventas.venta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResumen;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;

public record VentaResponse(
        Long id,
        LocalDateTime fecha,
        TipoVenta tipo,
        String paisDestino,
        ClienteResumen cliente,
        EstadoVenta estado,
        BigDecimal total,
        VendedorResumen vendedor,
        List<DetalleVentaResponse> detalles) {
}
