package pe.edu.upeu.panaderia.finanzas.movimiento.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Operaciones financieras que necesita exclusivamente el módulo de ventas. */
public interface FinanzasVentaService {

    void registrarIngresoVenta(
            Long ventaId, LocalDateTime fecha, String tipoVenta, BigDecimal monto);

    void anularIngresoVenta(Long ventaId);
}
