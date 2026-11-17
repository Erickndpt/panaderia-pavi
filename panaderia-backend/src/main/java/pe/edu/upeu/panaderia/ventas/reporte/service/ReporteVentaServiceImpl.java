package pe.edu.upeu.panaderia.ventas.reporte.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;
import pe.edu.upeu.panaderia.ventas.venta.specification.VentaSpecifications;

@Service
@RequiredArgsConstructor
public class ReporteVentaServiceImpl implements ReporteVentaService {

    private final VentaRepository ventaRepository;

    @Override
    @Transactional(readOnly = true)
    public ReporteVentaResponse generar(LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        validarRangoFechas(fechaDesde, fechaHasta);
        List<Venta> ventas = ventaRepository.findAll(
                VentaSpecifications.enRangoFechas(fechaDesde, fechaHasta)
                        .and(VentaSpecifications.estadoIgual(EstadoVenta.REGISTRADA)));

        long cantidadLocales = 0;
        long cantidadExportaciones = 0;
        long cantidadVentas = 0;
        BigDecimal montoLocal = BigDecimal.ZERO;
        BigDecimal montoExportacion = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            if (venta.getEstado() != EstadoVenta.REGISTRADA) {
                continue;
            }
            cantidadVentas++;
            if (venta.getTipo() == TipoVenta.LOCAL) {
                cantidadLocales++;
                montoLocal = montoLocal.add(venta.getTotal());
            } else if (venta.getTipo() == TipoVenta.EXPORTACION) {
                cantidadExportaciones++;
                montoExportacion = montoExportacion.add(venta.getTotal());
            }
        }

        BigDecimal montoTotal = montoLocal.add(montoExportacion);

        return new ReporteVentaResponse(
                cantidadVentas,
                montoTotal,
                cantidadLocales,
                montoLocal,
                cantidadExportaciones,
                montoExportacion);
    }

    private void validarRangoFechas(LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new FiltroInvalidoException("fechaDesde no debe ser posterior a fechaHasta");
        }
    }
}