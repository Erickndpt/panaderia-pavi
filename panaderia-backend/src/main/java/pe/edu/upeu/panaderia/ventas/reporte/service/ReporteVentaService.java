package pe.edu.upeu.panaderia.ventas.reporte.service;

import java.time.LocalDateTime;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;

public interface ReporteVentaService {

    ReporteVentaResponse generar(LocalDateTime fechaDesde, LocalDateTime fechaHasta);
}