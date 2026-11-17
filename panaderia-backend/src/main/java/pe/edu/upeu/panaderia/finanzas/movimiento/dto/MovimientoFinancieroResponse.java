package pe.edu.upeu.panaderia.finanzas.movimiento.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.EstadoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.OrigenMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.TipoMovimiento;

public record MovimientoFinancieroResponse(
        Long id,
        LocalDateTime fecha,
        TipoMovimiento tipo,
        OrigenMovimiento origen,
        Long origenId,
        String concepto,
        BigDecimal monto,
        EstadoMovimiento estado) {
}