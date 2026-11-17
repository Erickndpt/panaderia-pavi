package pe.edu.upeu.panaderia.ventas.reporte.dto;

import java.math.BigDecimal;

public record ReporteVentaResponse(
        long cantidadVentas,
        BigDecimal montoTotal,
        long cantidadLocales,
        BigDecimal montoLocal,
        long cantidadExportaciones,
        BigDecimal montoExportacion) {
}